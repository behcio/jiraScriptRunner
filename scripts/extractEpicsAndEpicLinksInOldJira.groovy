/*
This script is to be used in Script Console.
It searches for all Epics in a project and then for all issues linked to epic via Epic Link
Next, results are saved to a List and exported to logs.
Uses older Jira (< 8.x.x) compatible JQL search
 */

import com.atlassian.jira.component.ComponentAccessor
import com.atlassian.jira.issue.search.SearchProvider
import com.atlassian.jira.jql.parser.JqlQueryParser
import com.atlassian.jira.web.bean.PagerFilter
import org.apache.log4j.Level
import org.apache.log4j.Logger

def log = Logger.getLogger("com.EpicExport")
log.setLevel(Level.DEBUG)

def findIssues(String jqlQuery) {
    def issueManager = ComponentAccessor.getIssueManager();
    def jqlQueryParser = ComponentAccessor.getComponent(JqlQueryParser.class);
    def searchProvider = ComponentAccessor.getComponent(SearchProvider.class);
    def user = ComponentAccessor.getJiraAuthenticationContext().getLoggedInUser();
    def query = jqlQueryParser.parseQuery(jqlQuery);

    //def results = searchProvider.search(query, user, PagerFilter.getUnlimitedFilter());
    //log.warn "issue count: ${results.getTotal()}";

    def results = searchProvider.search(query, user, PagerFilter.unlimitedFilter)
    results.issues.collect {
        it -> issueManager.getIssueObject(it.id)
    }
}

def jqlQuery = "project = PL and type = Epic and key > PL-5900"
def epicIssues = findIssues(jqlQuery);
//log.debug "Epics: ${epicIssues}"

epicIssues.each(){
    def issuesInEpicJqlQuery = "project = PL and 'Epic Link' = ${it}"
    def issuesInEpic = findIssues(issuesInEpicJqlQuery)
    def issuesInEpicList = []
    issuesInEpic.each{
        issuesInEpicList.add(it)
    }

    def logString
    //Extract only epics with issues in them. Others are not interesting
    if(issuesInEpicList.size() > 0){
        logString = "Epic and its issues: [\"${it}\""
        issuesInEpicList.each{ iss ->
            logString += ", \"${iss}\""
        }
        logString += "],"
        log.debug "${logString}"
    }

}