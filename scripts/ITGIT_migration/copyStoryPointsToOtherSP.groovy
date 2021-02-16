/*
This script is meant to be run in the script console.
This script copies Story Points from the "Story Points" fields to "Story points".
The copy is made only when "Story point" (the good one) field is empty.
The custom field IDs are taken from ITGIT Prod instance (the same are for test instance)
Also, a comment is added to each updated issue stating what happened. The sole reason behind
this is to create an event (EventType.ISSUE_COMMENTED_ID) that an issue was updated
so that e.g. JQL searches work properly for updated issues.

In order to customize the script to work with any project, you just need to change
the project variable in PARAMS section.
Additionally, you can enable/disable copying values from either of the ITGIT "bad"
Story Points fields. Check which of these are applicable to your project.

This script DOES change Jira content so be sure what you're doing.

Uses Jira 8.x.x compatible JQL search
 */


import com.atlassian.jira.component.ComponentAccessor
import com.atlassian.jira.event.type.EventDispatchOption
import com.atlassian.jira.issue.search.SearchProvider
import com.atlassian.jira.issue.search.SearchQuery
import com.atlassian.jira.issue.util.DefaultIssueChangeHolder
import com.atlassian.jira.jql.parser.JqlQueryParser
import com.atlassian.jira.web.bean.PagerFilter
import org.apache.log4j.Level
import org.apache.log4j.Logger
/*
!!! PARAMS !!!
*/
def project = "PL"
// So called Bad Story Points 1 are common in ITGIT
def copyFromBadStoryPoints1 = true
// This field is rarely used in ITGIT, disable if not applicable for project
def copyFromBadStoryPoints2 = false
/*
!!! END PARAMS !!!
*/

def log = Logger.getLogger("com.acme.CopyStoryPoints")
log.setLevel(Level.DEBUG)

def customFieldManager = ComponentAccessor.getCustomFieldManager()
def issueManager = ComponentAccessor.getIssueManager()
def changeHolder = new DefaultIssueChangeHolder()

def findIssues(String jqlQuery) {
    def issueManager = ComponentAccessor.issueManager;
    def user = ComponentAccessor.getJiraAuthenticationContext().getLoggedInUser();
    def jqlQueryParser = ComponentAccessor.getComponent(JqlQueryParser);
    def searchProvider = ComponentAccessor.getComponent(SearchProvider);
    def query = jqlQueryParser.parseQuery(jqlQuery);
    def searchQuery = SearchQuery.create(query, user);

    def results = searchProvider.search(searchQuery, PagerFilter.getUnlimitedFilter());
    //log.warn "issue count: ${results.getTotal()}";
    results.getResults().collect { res ->
        def doc = res.getDocument();
        def key = doc.get("key");
        def issue = ComponentAccessor.getIssueManager().getIssueObject(key);
        return issue;
    }
}

def goodStoryPointsField = customFieldManager.getCustomFieldObject("customfield_12400")
def badStoryPointsField1 = customFieldManager.getCustomFieldObject("customfield_43304")
def badStoryPointsField2 = customFieldManager.getCustomFieldObject("customfield_46715")

def commentManager = ComponentAccessor.getCommentManager()
def user = ComponentAccessor.getJiraAuthenticationContext().getUser()

def badSP1
def badSP2
def goodSP
def processedIssue

if(copyFromBadStoryPoints1){
    def jqlQuery1 = "project = ${project} AND (cf[43304] is not empty AND cf[12400] is empty)"
    def foundIssues1 = findIssues(jqlQuery1)
    foundIssues1.each{ iss ->
        badSP1 = iss.getCustomFieldValue(badStoryPointsField1)
        goodSP = iss.getCustomFieldValue(goodStoryPointsField)
        processedIssue = issueManager.getIssueObject(iss.getKey())
        log.debug "Issue: ${iss}, BadSP1: ${badSP1}, goodSP: ${goodSP}"
        processedIssue.setCustomFieldValue(goodStoryPointsField, badSP1)
        //Add comment about update
        commentManager.create(iss, user, "Copied Story Points (CF id: 43304) to Story points (CF id: 12400)", false)
        //Send an update event but suppress email notification
        issueManager.updateIssue(user, processedIssue, EventDispatchOption.ISSUE_UPDATED, false)
    }
}

if(copyFromBadStoryPoints2){
    def jqlQuery2 = "project = ${project} AND (cf[46715] is not empty AND cf[12400] is empty)"
    def foundIssues2 = findIssues(jqlQuery2)
    foundIssues2.each{ iss ->
        badSP2 = iss.getCustomFieldValue(badStoryPointsField2)
        goodSP = iss.getCustomFieldValue(goodStoryPointsField)
        processedIssue = issueManager.getIssueObject(iss.getKey())
        log.debug "Issue: ${iss}, BadSP2: ${badSP2}, goodSP: ${goodSP}"
        processedIssue.setCustomFieldValue(goodStoryPointsField, badSP2)
        //Add comment about update
        commentManager.create(iss, user, "Copied Story Points (CF id: 46715) to Story points (CF id: 12400)", false)
        //Send an update event but suppress email notification
        issueManager.updateIssue(user, processedIssue, EventDispatchOption.ISSUE_UPDATED, false)
    }
}
