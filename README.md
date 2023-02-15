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
  

# Setup


#### Prerequisites: Jdk 11, Git, Maven 3

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


# Running Kustvakt Server

<pre>
java -jar target/Kustvakt-full-[version].jar    
</pre>

will run Kustvakt full version with the example [kustvakt.conf](https://github.com/KorAP/Kustvakt/blob/master/full/src/main/resources/kustvakt.conf) configuration file included. See [Customizing kustvakt configuration](https://github.com/KorAP/Kustvakt/edit/master/README.md#customizing-kustvakt-configuration).

Kustvakt full version requires a Krill index and [LDAP configuration](https://github.com/KorAP/Kustvakt/wiki/LDAP-Setting). By default, Kustvakt uses the [sample-index](https://github.com/KorAP/Kustvakt/tree/master/sample-index) located in the parent directory of the jar file and [the embedded LDAP server](https://github.com/KorAP/Kustvakt/blob/master/full/src/main/resources/embedded-ldap-example.conf) example.

### Running a custom Spring XML configuration

To run Kustvakt with a custom Spring XML configuration file, it must be included in the classpath. For instance:
```custom-spring-config.xml``` is located in ```config``` folder. 
The ```config``` folder must be included in the classpath with ```-cp``` command.

<pre>
cd target/
java -cp Kustvakt-full-[version].jar:config de.ids_mannheim.korap.server.KustvaktServer 
--spring-config custom-spring-config.xml
</pre>

### Generating an OAuth2 super client

An OAuth2 super client is required to be able to use web services that require user authentication. Kustvakt can generate a super client automatically. See  [Setting Initial Super Client for User Authentication](https://github.com/KorAP/Kustvakt/wiki/Setting-Initial-Super-Client-for-User-Authentication). 


# Web-services

All web-services including their usage examples are described in the [wiki](https://github.com/KorAP/Kustvakt/wiki).

Some request examples:

* search

```
curl 'http://localhost:8089/api/v1.0/search?q=Wasser&ql=poliqarp'
```

* search public metadata

```
curl 'http://localhost:8089/api/v1.0/search?q=Wasser&ql=poliqarp&fields=textSigle,title,availablility&access-rewrite-disabled=true'
```

* match info

```
curl 'http://localhost:8089/api/v1.0/corpus/GOE/AGA/01784/p4145-4146?foundry=opennlp'
```



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



### Advanced Setup

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
