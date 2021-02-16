/*
 This script shows how a transition can trigger cloning of another issue. It uses very similar features to those already introduced in
 createManySubtasks.groovy and createTestIssueLinkedToEpic.groovy

 The only new feature is usage of cloneIssue method in the final 'if' of this script

 */

import com.atlassian.jira.component.ComponentAccessor
import com.atlassian.jira.issue.MutableIssue
import com.atlassian.jira.issue.search.SearchProvider
import com.atlassian.jira.jql.parser.JqlQueryParser
import com.atlassian.jira.web.bean.PagerFilter
import org.apache.log4j.Level
import org.apache.log4j.Logger
//Setting up a Logger.
//NOTE: tutorials suggest that defining log is not necessary which is contrary to my experience.
def log = Logger.getLogger("Manual_Regression_Tests")
log.setLevel(Level.DEBUG)

def originIssueSummary = "Manual regression tests for Releases"
def issueManager = ComponentAccessor.getIssueManager()
def user = ComponentAccessor.getJiraAuthenticationContext().getLoggedInUser()
def issueFactory = ComponentAccessor.getIssueFactory()
def customFieldManager = ComponentAccessor.getCustomFieldManager()
def jqlQueryParser = ComponentAccessor.getComponent(JqlQueryParser.class)
def searchProvider = ComponentAccessor.getComponent(SearchProvider.class)
def epicLink = customFieldManager.getCustomFieldObjectByName('Epic Link')
//Get Epic issue object. Issue variable is associated with epic because this script is run during an Epic's status transition
def epicIssue = issueManager.getIssueObject(issue.getKey())

//If a Manual regression tests issue already exists in epic, abort script.
def existingIssueJqlQery = jqlQueryParser.parseQuery("project = PL AND summary ~ '${originIssueSummary}' AND type = Test AND labels in ('Release') AND 'Epic Link' = ${epicIssue.getKey()}")
def existingIssueResults = searchProvider.search(existingIssueJqlQery, user, PagerFilter.getUnlimitedFilter())

if(existingIssueResults.getIssues()[0]){
    log.debug("Manual Regression Tests issue already exists in this epic")
    return
}

//Make sure original issue exists in JIRA. If not, abort.
def originIssueJqlQuery = jqlQueryParser.parseQuery("project = pl AND summary ~ '${originIssueSummary}' AND type = test AND labels in ('Release') AND 'Epic Link' is EMPTY")
def originIssueResults = searchProvider.search(originIssueJqlQuery, user, PagerFilter.getUnlimitedFilter())

if(originIssueResults.getIssues()[0]){
    def originIssue = originIssueResults.getIssues()[0]
    //Set clone's properties
    MutableIssue clone = issueFactory.cloneIssue(originIssue)
    clone.setCustomFieldValue(epicLink, epicIssue)
    def cloneIssueParams = ["issue" : clone] as Map<String,Object>
    //Create clone
    issueManager.createIssueObject(user, cloneIssueParams)
}
else{
    log.debug("Manual Regression Tests issue not found")
    return
}