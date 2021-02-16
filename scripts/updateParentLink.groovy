/*
This script is to be used in the Script Console and is used to update Parent Link
of the child issue to that of the parent issue.
Updating parent links is very similar to updating epic links - the difference lies in using mutableIssue

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

def childIssue = issueManager.getIssueByCurrentKey("PL-783")
def parentIssue = issueManager.getIssueByCurrentKey("FVC-3")

def parentLinkField = ComponentAccessor.customFieldManager.getCustomFieldObjects(childIssue).findByName("Parent Link")
def parentLinkFieldType = parentLinkField.getCustomFieldType()

def mutableParentIssue = parentLinkFieldType.getSingularObjectFromString(parentIssue.key)
def mutableIssue = issueManager.getIssueObject(childIssue.id)

assert parentLinkField : "Could not find custom field with name Parent Link"
parentLinkField.updateValue(null, mutableIssue, new ModifiedValue(parentLinkField, mutableParentIssue), new DefaultIssueChangeHolder())