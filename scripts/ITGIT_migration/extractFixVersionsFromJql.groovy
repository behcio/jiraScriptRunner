/*
This script is to be used in Script Console.
This script extracts fix versions from all issues found using provided JQL query and
returns results as a list of lists, where each inner list is of format:
[issue, fixVersion1, fixVersion2...]
Inner lists are generated only for issues that have at least one fixVersion assigned

In order to customize the script to work with desired query, you just need to change
jqlQuery in PARAMS section.

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
def jqlQuery = "project = PL and key > PL-5000"
/*
!!! END PARAMS !!!
*/

def issueManager = ComponentAccessor.issueManager;
def log = Logger.getLogger("com.FixVersionsExport")
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

def issuesAndFixVersions = []

def foundIssues = findIssues(jqlQuery);

foundIssues.each(){ iss ->
    def issString = "\"" + iss + "\""
    def issueAndFixVersions = [issString]

    def versions = iss.getFixVersions()
    List quotedVersions = versions.collect{ '"' + it + '"'}

    if(quotedVersions.size() > 0){
        //log.debug "${it}, ${quotedVersions}"
        quotedVersions.each(){ ver ->
            issueAndFixVersions << ver
        }
        issuesAndFixVersions << issueAndFixVersions
    }
}

return issuesAndFixVersions