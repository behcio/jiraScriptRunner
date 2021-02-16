/*
This script is to be used in the Script Console.
This script is used to set Parent Links for many issues which should be provided as a List of Lists
where first element of each inner list is the target (parent) issue.

In order to customize the script to work with any project, you just need to change
parentsAndChildren in PARAMS section. This is the input data for this script. Example provided (FVC
issues are the future parents and PL issues are the future children).

This script DOES change Jira content so be sure what you're doing.
Apparently this script is quite heavy for Jira to process if your input data is very large (few
thousand issues). It might be a good idea to split input data into smaller chunks and execute the
script a few times.

This script is compatible with JIRA 8.x.x - in terms of setting parent links.
 */

import com.atlassian.jira.component.ComponentAccessor
import com.atlassian.jira.issue.ModifiedValue
import com.atlassian.jira.issue.util.DefaultIssueChangeHolder
import org.apache.log4j.Level
import org.apache.log4j.Logger

/*
!!! PARAMS !!!
*/
def parentsAndChildren = [["FVC-391", "PL-6531", "PL-6530"], ["FVC-378", "PL-6529", "PL-6528", "PL-6527", "PL-6526"], ["FVC-393"]]
/*
!!! END PARAMS !!!
*/

def log = Logger.getLogger("com.parentLinkImport")
log.setLevel(Level.DEBUG)

def customFieldManager = ComponentAccessor.getCustomFieldManager()
def issueManager = ComponentAccessor.getIssueManager()

parentsAndChildren.each{ parentAndChildrenRow ->
    log.debug "parentAndChildrenRow: ${parentAndChildrenRow}"
    if(parentAndChildrenRow.size() > 1){
        def parentIssue = issueManager.getIssueByCurrentKey(parentAndChildrenRow[0])
        parentAndChildrenRow.remove(0)
        //log.debug "epic: ${parentIssue}"
        parentAndChildrenRow.each{ iss ->
            def childIssue = issueManager.getIssueByCurrentKey(iss)
            //log.debug "Parent and child issue: ${parentIssue} ${iss}"
            def parentLinkField = ComponentAccessor.customFieldManager.getCustomFieldObjects(childIssue).findByName("Parent Link")
            def parentLinkFieldType = parentLinkField.getCustomFieldType()
            def mutableParentIssue = parentLinkFieldType.getSingularObjectFromString(parentIssue.key)

            assert parentLinkField : "Could not find custom field with name Parent Link"
            parentLinkField.updateValue(null, childIssue, new ModifiedValue(parentLinkField, mutableParentIssue), new DefaultIssueChangeHolder())
        }
    }
}

return "done"