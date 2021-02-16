/*
 This is an example of a script used in "Additional issue actions" field found in configuration for a "Clones an issue, and links" post-function.
 It is important to understand that this script is executed AFTER an issue is created (if Condition is passed, that is). This means that adding conditions
 that are meant to allow/deny creation of an issue to this script won't work.

 Following script is very similar to createTestIssueLinkedToEpic.groovy. The additional features are:
 * Adding labels to the created issue
 * Setting story points value

 This script is performed during an Epic's Refinement --> Ready for Implementation transition

*/

import com.atlassian.jira.component.ComponentAccessor
import com.atlassian.jira.issue.ModifiedValue
import com.atlassian.jira.issue.label.LabelManager
import com.atlassian.jira.issue.util.DefaultIssueChangeHolder
import org.apache.log4j.Level
import org.apache.log4j.Logger

def log = Logger.getLogger("com.acme.CreateSubtask")
log.setLevel(Level.DEBUG)

def customFieldManager = ComponentAccessor.getCustomFieldManager()
def issueManager = ComponentAccessor.getIssueManager()
def epicLink = customFieldManager.getCustomFieldObjectByName('Epic Link')
//There are 2 custom fields called Story Points in our JIRA, hence an ID is used to find the proper object
def storyPointsField = customFieldManager.getCustomFieldObject("customfield_10002")
def epicIssue = issueManager.getIssueObject(sourceIssue.getKey())
def summaryCore = 'Test Execution'
//User is required for adding labels
def user = ComponentAccessor.getJiraAuthenticationContext().getLoggedInUser()

if(epicIssue){
    issue.setCustomFieldValue(epicLink, epicIssue)
    issue.summary = summaryCore + ': ' + epicIssue.getSummary()
    issue.description = "Test Execution for the whole epic. Assign all tests from this epic here"
}

//Very important: labels need to be added after creating an issue. This doAfterCreate clause actually helps do that in the same script. Otherwise adding labels
//would cause errors. I'm not sure if it's the same case for story points.
doAfterCreate = {
    def labelManager = ComponentAccessor.getComponent(LabelManager)
    def changeHolder = new DefaultIssueChangeHolder()
    labelManager.addLabel(user, issue.getId(), 'EpicTestExec', false)
    //It might be tempting to ise setCustomFieldValue just like for epics but it doesn't work in this case. Use this instead:
    storyPointsField.updateValue(null, issue, new ModifiedValue(issue.getCustomFieldValue(storyPointsField),(double) 0), changeHolder)
}