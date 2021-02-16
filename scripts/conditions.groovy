/*
 This is an example script for Condition field, which can be found in a "Clones an issue, and links" post function
 By default, a blank Condition script returns true which means the post function will be executed.
 A Condition script can be a simple boolean expression, e.g.
    (issue.projectObject.key == 'PL' && ("Release" in issue.labels*.label))

 However, if a more advanced script is required you can use a boolean variable, manipulate it and call it - like in the script below.

 NOTE: various tutorials suggest that manipulating passesCondition's value is sufficient and its end value will allow the post function to trigger or not.
 However, my experience shows that calling this variable at the finish is necessary.

 This particular condition script is executed during an epic's Draft --> Refinement transition
*/

import com.atlassian.jira.component.ComponentAccessor
import com.atlassian.jira.issue.search.SearchProvider
import com.atlassian.jira.jql.parser.JqlQueryParser
import com.atlassian.jira.web.bean.PagerFilter

//passesCondition variable is predefined in Condition scripts, so there's no need to define it
//Although ScriptRunner's Condition script window will underline it with an error message but it can be ignored
passesCondition = false;

//If project key is PL AND labels field contains "Release", issue should be cloned (created)
if(issue.projectObject.key == 'PL' && ("Release" in issue.labels*.label)){
    passesCondition = true;
}

def issueManager = ComponentAccessor.getIssueManager();
def jqlQueryParser = ComponentAccessor.getComponent(JqlQueryParser.class);
def searchProvider = ComponentAccessor.getComponent(SearchProvider.class);
def user = ComponentAccessor.getJiraAuthenticationContext().getLoggedInUser();
def summaryCore = 'Regression Tests (PL Engine and Flow Runner)';
//Get epic issue object. issue variable is associated with epic because script is executed during an epic's status transition
def epicIssue = issueManager.getIssueObject(issue.getKey());

//Perform a JIRA search to check if target issue exists
def jqlQuery = jqlQueryParser.parseQuery("project = PL AND type = test AND 'Epic Link' = ${epicIssue.getKey()} AND summary ~ '${summaryCore}' ORDER BY key DESC");
def results = searchProvider.search(jqlQuery, user, PagerFilter.getUnlimitedFilter());

//Abort creation if target issue already exists
if(results.getIssues()[0]){
    passesCondition = false;
}

//Call passesCondition
passesCondition;