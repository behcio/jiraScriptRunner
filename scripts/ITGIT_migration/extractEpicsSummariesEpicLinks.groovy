/*
This script is to be used in Script Console.
It searches for all Epics in a project and then for all issues linked to epic via Epic Link
Next, results are gathered in a list of lists, where each inner list holds epic key as first element
and linked issues as all others. The whole list is then shown in Result tab.
Alternatively, list can be exported to logs - uncomment proper section.
Results need to be extracted as quoted strings in order to be usable in other scripts as inputs.
It is highly recommended to save the results in a separate text file.

As an additional option, epic summaries can be added to the exports as well.

In order to customize the script to work with any project, you just need to change
sourceProjectKey in PARAMS section.

This script does not change any content in Jira.

Uses Jira 8.x.x compatible JQL search
 */

import com.atlassian.jira.component.ComponentAccessor
import com.atlassian.jira.issue.search.SearchProvider
import com.atlassian.jira.issue.search.SearchQuery
import com.atlassian.jira.jql.parser.JqlQueryParser
import com.atlassian.jira.web.bean.PagerFilter
import org.apache.log4j.Level
import org.apache.log4j.Logger

/*
!!! PARAMS !!!
*/
//Export epic summaries - they will take second place on the list after epic key in inner lists
//Note: If you plan to later import epic links using the results of this script, turn this option
//off as the import script only uses Epic and Children keys
def exportEpicSummaries = true
//Export epic links
def exportEpicLinks = true
//Key of project to extract data from
def sourceProjectKey = "PL"
/*
!!! END PARAMS !!!
*/

def issueManager = ComponentAccessor.issueManager;
def log = Logger.getLogger("com.EpicAndEpicLinksExport")
log.setLevel(Level.DEBUG)

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

def epicsAndLinkedIssues = []

def jqlQuery = "project = ${sourceProjectKey} and type = Epic";
def epicIssues = findIssues(jqlQuery);
//log.debug "Epics: ${epicIssues}"

epicIssues.each(){
    def issuesInEpicJqlQuery = "project = ${sourceProjectKey} and 'Epic Link' = ${it}"
    def issuesInEpic = findIssues(issuesInEpicJqlQuery)
    def itString = "\"" + it + "\""
    def epicAndIssuesList = [itString]

    if(exportEpicSummaries){
        def epicSummary = issueManager.getIssueObject(it.getKey()).getSummary()
        epicSummary = epicSummary.replaceAll(/"/, /\\\\\\\"/)
        epicSummary = "\"" + epicSummary + "\""
        epicAndIssuesList << epicSummary
    }

    if(exportEpicLinks) {
        issuesInEpic.each { iss ->
            def issString = "\"" + iss + "\""
            epicAndIssuesList << issString
        }
    }

    //Extract only epics with issues in them. Others are not interesting
    if(epicAndIssuesList.size() > 1){
        epicsAndLinkedIssues << epicAndIssuesList
    }

    /*
    //This section exports epics and epic links to logs

    def logString
    //Extract only epics with issues in them. Others are not interesting
    if(epicAndIssuesList.size() > 1){
        logString = "Epic and its issues: [${epicAndIssuesList[0]}"
        epicAndIssuesList.remove(0)
        epicAndIssuesList.each{ iss ->
            logString += ", ${iss}"
        }
        logString += "],"
        log.debug "${logString}"
    }
    */
}

return epicsAndLinkedIssues