package sandboxes.solrplugins

import org.apache.log4j.BasicConfigurator
import org.apache.log4j.Logger
import org.apache.solr.client.solrj.SolrServer;
import org.apache.solr.client.solrj.impl.LBHttpSolrServer
import org.apache.solr.common.SolrInputDocument

import com.sun.syndication.io.SyndFeedInput

class FeedDriver {

    public static void main(String[] args) {
        
        BasicConfigurator.resetConfiguration()
        BasicConfigurator.configure()

        def cli = new groovy.util.CliBuilder(
            usage: "java ${FeedDriver.class.getName()} [feed url] ${System.getProperty('line.separator')}" +
            "Parses an Atom or RSS feed and submits entries to the cluster for indexing.",
            header: "options")
        cli.n(longOpt: "noIndex", "No index - download and parse only.")

        OptionAccessor options = cli.parse(args)
        if (!options || !args) {
            cli.usage()
            System.exit(1)
        }
        
        if (!options.arguments()) {
            throw new IllegalArgumentException("Nothing to do.")
        }

        SolrServer solrCluster = new LBHttpSolrServer("http://localhost:8983/solr", "http://localhost:7983/solr")
        int commitWithinMsecs = 10000
        def docs = []

        options.arguments().each { arg -> 
            def syndFeed = new SyndFeedInput().build(new InputStreamReader(
                new URL(arg).newInputStream()))
            syndFeed.entries.each { entry ->
                def doc = new SolrInputDocument()
                doc.setField("id", entry.link)
                doc.setField("uri", entry.link)
                doc.setField("media-type", "text/html") // for now
                
                if (options.n) {
                    Logger.getRootLogger().info("Not indexing: $doc")
                } else {
                    Logger.getRootLogger().info("Indexing: $doc")
                    docs << doc
                }
            }
        }
        if (!options.n) {
            solrCluster.add(docs, commitWithinMsecs)
        }
    }
}
