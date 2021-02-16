/*
This script is to be used in Script Console.
It searches two projects for issues with common (exact) Summary. This is useful when epics from basic Jira
setup are migrated to a Program level project in SAFe Jira setup in order to connect old epics with new features.
This can later be used e.g. for restoring old Epic Links into Parent links.
This particular script assumes old Basic project is intact and Epics were copied (not moved) to new Program project.
Results need to be extracted as quoted strings in order to be usable in other scripts as inputs

Uses Jira 8.x.x compatible JQL search
 */

import com.atlassian.jira.component.ComponentAccessor
import com.atlassian.jira.issue.search.SearchProvider
import com.atlassian.jira.issue.search.SearchQuery
import com.atlassian.jira.jql.parser.JqlQueryParser
import com.atlassian.jira.web.bean.PagerFilter
import org.apache.log4j.Level
import org.apache.log4j.Logger

def log = Logger.getLogger("com.BetweenProjectsSearchBySummary")
log.setLevel(Level.DEBUG)

def issueManager = ComponentAccessor.getIssueManager()

def findIssues(String jqlQuery) {
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

def sourceProject = "PLSBOXT"
def targetProject = "PLSBOXA"
def sourceAndTargetKeys = []
def sourceKeysAndSummaries = []

def jqlQuerySourceEpics = "project = ${sourceProject} and type = Story order by key ASC"
def epicIssuesSearchResult = findIssues(jqlQuerySourceEpics);

epicIssuesSearchResult.each(){
    def epicSummary = issueManager.getIssueObject(it.getKey()).getSummary()
    //Replace double quotes with escape-double quotes so they don't cause problems in later JQL searches
    epicSummary = epicSummary.replaceAll(/"/, /\\\"/)
    epicSummary = "\"" + epicSummary + "\""
    //log.debug "Origin issue: ${it}; Summary: ${epicSummary}"
    def epicKeyAndSummary = []
    epicKeyAndSummary << it << epicSummary
    sourceKeysAndSummaries.add(epicKeyAndSummary)
}

log.debug "sourceKeysAndSummaries: ${sourceKeysAndSummaries}"

sourceKeysAndSummaries.each{ keyAndSummaryRow ->
    def sourceAndTarget = []
    //This JQL query uses Enhanced Search provided by Adaptavist ScriptRunner plugin
    def targetJqlQuery = "issueFunction in issueFieldExactMatch(\"project = ${targetProject}\", \"summary\", ${keyAndSummaryRow[1]})"
    def targetIssueQueryResults = findIssues(targetJqlQuery)

    if(targetIssueQueryResults.size() == 0){
        log.warn "No corresponding issue found in target project for ${keyAndSummaryRow[0]}, summary: ${keyAndSummaryRow[1]}"
    }
    else if(targetIssueQueryResults.size() > 1){
        log.warn "More than 1 issue found in target project for ${keyAndSummaryRow[0]}, summary: ${keyAndSummaryRow[1]}"
    }
    else{
        def targetIssue = issueManager.getIssueObject(targetIssueQueryResults[0].getKey())
        sourceKeyString = "\"" + keyAndSummaryRow[0] + "\""
        targetIssueString = "\"" + targetIssue + "\""
        sourceAndTarget << sourceKeyString << targetIssueString
        sourceAndTargetKeys << sourceAndTarget
        //log.debug "sourceAndTargetKeys: ${sourceAndTargetKeys}"
    }
}

//log.debug "${sourceAndTargetKeys}"
return sourceAndTargetKeys
