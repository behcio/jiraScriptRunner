/*
This script is to be used in the Script Console.
This script is used to set Epic Links for many issues which should be provided as a List of Lists
where first element of each inner list is the target epic.

This script is compatible with JIRA 8.x.x - in terms of setting epic links. Most likely the old appoach using
setCustomFieldValue method doesn't work anymore.
 */



import com.atlassian.jira.component.ComponentAccessor
import com.atlassian.jira.issue.ModifiedValue
import com.atlassian.jira.issue.util.DefaultIssueChangeHolder
import org.apache.log4j.Level
import org.apache.log4j.Logger

def log = Logger.getLogger("com.EpicLinkImport")
log.setLevel(Level.DEBUG)

def customFieldManager = ComponentAccessor.getCustomFieldManager()
def issueManager = ComponentAccessor.getIssueManager()

def epicsAndEpicLinks = [
    ["PL-6494", "PL-6496", "PL-6495"],
    ["PL-6483", "PL-6485"]
    ]

epicsAndEpicLinks.each{ epicAndLinksRow ->
    log.debug "epicAndLinksRow: ${epicAndLinksRow}"
    def epicIssue = issueManager.getIssueByCurrentKey(epicAndLinksRow[0])
    epicAndLinksRow.remove(0)
    log.debug "epic: ${epicIssue}"
    epicAndLinksRow.each{ iss ->
        def targetIssue = issueManager.getIssueByCurrentKey(iss)
        log.debug "issue: ${epicIssue} ${iss}"
        def targetField = ComponentAccessor.customFieldManager.getCustomFieldObjects(targetIssue).findByName("Epic Link")
        assert targetField : "Could not find custom field with name Epic Link"
        targetField.updateValue(null, targetIssue, new ModifiedValue(targetIssue.getCustomFieldValue(targetField), epicIssue), new DefaultIssueChangeHolder())
    }
}