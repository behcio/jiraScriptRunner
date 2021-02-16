/*
This script was written in Script Console. It executes a JQL query and returns results.
What is special about this approach (which is different than in other files in this repo) is
that it is adapted to JIRA version 8.x.x which apparently breaks previous implementations.
Whether this implementation is viable for older JIRA versions remains to be investigated.
 */

import com.atlassian.jira.component.ComponentAccessor
import com.atlassian.jira.issue.search.SearchProvider
import com.atlassian.jira.issue.search.SearchQuery
import com.atlassian.jira.jql.parser.JqlQueryParser
import com.atlassian.jira.web.bean.PagerFilter

def findIssues(String jqlQuery) {
    def issueManager = ComponentAccessor.issueManager;
    def user = ComponentAccessor.getJiraAuthenticationContext().getLoggedInUser();
    def jqlQueryParser = ComponentAccessor.getComponent(JqlQueryParser);
    def searchProvider = ComponentAccessor.getComponent(SearchProvider);
    def query = jqlQueryParser.parseQuery(jqlQuery);
    def searchQuery = SearchQuery.create(query, user);

    def results = searchProvider.search(searchQuery, PagerFilter.getUnlimitedFilter());
    log.warn "issue count: ${results.getTotal()}";
    results.getResults().collect { res ->
        def doc = res.getDocument();
        def key = doc.get("key");
        def issue = ComponentAccessor.getIssueManager().getIssueObject(key);
        return issue;
    }
}
def jqlQuery = "project = PL and 'Epic Link' = PL-6256";
def issues = findIssues(jqlQuery);