/*
 This is an example of a script used in "Additional issue actions" field found in configuration for a "Clones an issue, and links" post-function.
 It is important to understand that this script is executed AFTER an issue is created (if Condition is passed, that is). This means that adding conditions
 that are meant to allow/deny creation of an issue to this script won't work.

 Following script is meant to set the newly created issue's Summary, Description and Epic Link fields. Since it's supposed to be a regression test issue for a release epic,
 its summary should contain release version. This is why regular expressions are used in this script.

 This script is performed during an Epic's Draft --> Refinement transition
 */

import com.atlassian.jira.component.ComponentAccessor

def customFieldManager = ComponentAccessor.getCustomFieldManager();
def issueManager = ComponentAccessor.getIssueManager();
def epicLink = customFieldManager.getCustomFieldObjectByName('Epic Link');
def epicIssue = issueManager.getIssueObject(sourceIssue.getKey());
def summaryCore = 'Regression Tests (PL Engine and Flow Runner)';

//Checking if the epic issue contains a release version in its summary
def pattern = /\d+\.\d+\.\d+/;
def matcher = (epicIssue.getSummary() =~ pattern);

//If the issue contains a release version in its summary, add it to target test issue's summary
if(epicIssue){
    issue.setCustomFieldValue(epicLink, epicIssue);
    if(matcher.find()){
        issue.summary = matcher.group() + ' ' + summaryCore;
        issue.description = "h2. Example text\n* With typical JIRA formatting"
    }
    else{
        issue.summary = summaryCore;
        issue.description = "h2. Example text\n* With typical JIRA formatting"
    }
}