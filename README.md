![Kustvakt](https://raw.githubusercontent.com/KorAP/Kustvakt/master/misc/kustvakt.png)

[![DOI](https://zenodo.org/badge/104361763.svg)](https://zenodo.org/badge/latestdoi/104361763)

Kustvakt is a user and policy management component for KorAP (Diewald et al., 2016). It manages user access to resources (i.e. corpus data) typically bound with some licensing schemes. The licensing schemes of IDS resources provided through KorAP (DeReKo) are very complex involving the user access location and purposes (Kupietz & Lüngen, 2014). To manage user access to resources, Kustvakt performs query rewriting with document restrictions (Bański et al., 2014).

Kustvakt acts as a middleware in KorAP binding other components, such as [Koral](https://github.com/KorAP/Koral) a query serializer and [Krill](https://github.com/KorAP/Krill) a search component, together. As the KorAP's API provider, it provides services, e.g. searching and retrieving annotation data of a match/hit, that can be used by a KorAP client, e.g. [Kalamar](https://github.com/KorAP/Kalamar) (a KorAP web user interface) and [KorapSRU](https://github.com/KorAP/KorapSRU) (the CLARIN FCS endpoint for KorAP).


# Versions
* <b>Kustvakt lite version</b>
  
  provides basic services including search, match info, statistic and annotation services, without user and policy management.

* <b>Kustvakt full version</b>
  
  provides user and policy management and extended services, in addition to the basic services. This version requires a database (Sqlite is provided) and an LDAP system ([UnboundID InMemoryDirectoryServer](https://github.com/pingidentity/ldapsdk) is provided) for user authentication.
  
Recent changes on the project are described in the change logs (Changes files).
  
# Web-services

Web-services including their usage examples are described in the [wiki](https://github.com/KorAP/Kustvakt/wiki).


# Setup


Prerequisites: Jdk 11, Git, Maven 3

Clone the latest version of Kustvakt
<pre>
git clone git@github.com:KorAP/Kustvakt.git
</pre>

Since Kustvakt requires Krill and Koral, please install [Krill](https://github.com/KorAP/Krill) and [Koral](https://github.com/KorAP/Koral) in your maven local repository according to the required versions specified in ```Kustvakt/core/pom.xml```. 

Install Kustvakt-core in your maven local repository
<pre>
cd Kustvakt/core
mvn clean install
</pre>

Package Kustvakt full version
<pre>
cd ../full
mvn clean package
</pre>
The jar file is located in the ```target/``` folder.

Package Kustvakt lite version
<pre>
cd ../lite
mvn clean package
</pre>
The jar file is located in the ```target/``` folder.

# Customizing Kustvakt configuration

Copy the default Kustvakt configuration file (e.g. ```full/src/main/resources/kustvakt.conf``` or ```lite/src/main/resources/kustvakt-lite.conf```), to the same  folder as the Kustvakt jar files  (```/target```). Please do not change the name of the configuration file.

### Setting Index Directory

Set krill.indexDir in the configuration file to the location of your Krill index (relative path to the jar). In Kustvakt's root directory, there is a sample index, e.g.
<pre>krill.indexDir = ../../sample-index</pre>


### Changing Kustvakt Server Port and Host

	server.port = 8089
	server.host = localhost

### Setting Default Foundries

The following properties define the default foundries used for specific layers. 
For instance in a rewrite, a default foundry may be added to a Koral query
missing a foundry.   
   

	default.foundry.partOfSpeech = tt
	default.foundry.lemma = tt
	default.foundry.orthography = opennlp
	default.foundry.dependency = malt
	default.foundry.constituent = corenlp
	default.foundry.morphology = marmot
	default.foundry.surface = base


# Running Kustvakt Server
Requires ```kustvakt.conf``` or ```kustvakt-lite.conf``` in the same folder as the jar file. Otherwise assuming sample-index located in the parent directory of the jar file.

Kustvakt full version requires [LDAP configuration](https://github.com/KorAP/Kustvakt/wiki/LDAP-Setting). Kustvakt full version can be run without an LDAP system, but user authentication functions and services cannot be used. Only services for guest/demo user would be available.

<pre>
java -jar target/Kustvakt-[lite/full]-[version].jar    
</pre>

To run Kustvakt with a custom spring XML configuration file, it must be included in the classpath. For instance:
```custom-spring-config.xml``` is located in ```config``` folder. 
The ```config``` folder must be included in the classpath with ```-cp``` command.

<pre>
cd target/
java -cp Kustvakt-full-[version].jar:config de.ids_mannheim.korap.server.KustvaktServer 
--spring-config custom-spring-config.xml
</pre>


# Shutting down Kustvakt Server

Kustvakt server can be shut down by sending a POST request with a shutdown token. When Kustvakt server is started, a shutdown token is automatically generated and written to a ```shutdownToken``` file with the following format:

<pre>
token=[shutdown-token]
</pre>

A shutdown request can be sent as follows.

<pre>
curl -H "Content-Type: application/x-www-form-urlencoded" 
"http://localhost:8089/shutdown" -d @shutdownToken  
</pre>

# Advanced Setup

Advanced setup such as LDAP configurations, setting a test environment, database properties and mail configurations for email notifications, are described in the [wiki](https://github.com/KorAP/Kustvakt/wiki).

# License

Kustvakt is published under the [BSD-2 License](https://raw.githubusercontent.com/KorAP/Kustvakt/master/LICENSE). It is developed as part of [KorAP](https://korap.ids-mannheim.de/), the Corpus Analysis Platform at the [Leibniz Institute for the German Language (IDS)](https://www.ids-mannheim.de/),
member of the [Leibniz Association](https://www.leibniz-gemeinschaft.de).

# Contributions

Contributions to Kustvakt are very welcome!

Ideally, any contributions should be committed via [KorAP Gerrit server](https://korap.ids-mannheim.de/gerrit/) to facilitate code reviewing (see [Gerrit Code Review - A Quick Introduction](https://korap.ids-mannheim.de/gerrit/Documentation/intro-quick.html)). However, we are also happy to accept comments and pull requests via GitHub.

Please note that unless you explicitly state otherwise any contribution intentionally submitted for inclusion into Kustvakt shall –	
as Kustvakt itself – be under the [BSD-2 License](https://raw.githubusercontent.com/KorAP/Kustvakt/master/LICENSE).

# Publication

Diewald, Nils/Hanl, Michael/Margaretha, Eliza/Bingel, Joachim/Kupietz, Marc/Bański, Piotr/Witt, Andreas (2016):
    KorAP Architecture – Diving in the Deep Sea of Corpus Data. In: Calzolari, Nicoletta/Choukri, Khalid/Declerck, Thierry/Goggi, Sara/Grobelnik, Marko/Maegaard, Bente/Mariani, Joseph/Mazo, Helene/Moreno, Asuncion/Odijk, Jan/Piperidis, Stelios (Hrsg.): Proceedings of the Tenth International Conference on Language Resources and Evaluation (LREC 2016), Portorož, Slovenia. Paris: European Language Resources Association (ELRA), 2016. S. 3586-3591.

Bański, Piotr/Diewald, Nils/Hanl, Michael/Kupietz, Marc/Witt, Andreas (2014):
    Access Control by Query Rewriting. The Case of KorAP. In: Proceedings of the Ninth Conference on International Language Resources and Evaluation (LREC’14). European Language Resources Association (ELRA), 2014. S. 3817-3822.


# References

Kupietz, Marc/Lüngen, Harald (2014): Recent Developments in DeReKo. In: Calzolari, Nicoletta et al. (eds.): Proceedings of the Ninth International Conference on Language Resources and Evaluation (LREC'14). Reykjavik: ELRA, 2378-2385.
