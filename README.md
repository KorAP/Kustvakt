![Kustvakt](https://raw.githubusercontent.com/KorAP/Kustvakt/master/misc/kustvakt.png)

Kustvakt is a user and policy management component for KorAP (Diewald et al., 2016). It manages user access to resources (i.e. corpus data) that is typically bound with some licensing schemes. The licensing schemes of the IDS resources provided through KorAP (DeReKo) are very complex involving the access location and purposes (Kupietz & Lüngen, 2014). To manage user access to resources, Kustvakt performs query rewriting with document restrictions (Bański et al., 2014).

Kustvakt acts as a middleware in KorAP binding other components, such as [Koral](https://github.com/KorAP/Koral) a query serializer and [Krill](https://github.com/KorAP/Krill) a search component, together. As KorAP's API provider, it provides services, e.g. searching and retrieving annotation data of a match/hit, that can be used by a client, e.g. [Kalamar](https://github.com/KorAP/Kalamar) (a KorAP web user interface) and [KorapSRU](https://github.com/KorAP/KorapSRU) (the CLARIN FCS endpoint for KorAP).

# Versions
* <b>Kustvakt lite version</b>
  
  provides basic search and match info services without user and policy management.

* <b>Kustvakt full version</b>
  
  provides user and policy management and extended services (e.g. resource and annotation services) in addition to the basic services. This version requires a database (Sqlite is provided) and an LDAP system for user authentication.
  
# Web-services

Web-services including their usage examples are described in the [wiki](https://github.com/KorAP/Kustvakt/wiki).


# Setup


Prerequisites: Jdk 1.8, Git, Maven 3

Clone the latest version of Kustvakt
<pre>
git clone git@github.com:KorAP/Kustvakt.git
</pre>

Since Kustvakt requires Krill and Koral, please install [Krill](https://github.com/KorAP/Krill) and [Koral](https://github.com/KorAP/Koral) in your maven local repository.
Adjust the versions of Krill and Koral in ```Kustvakt/core/pom.xml``` 
according to the versions in 
```Koral/pom.xml```
 and 
 ```Krill/pom.xml```.

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

If there are errors regarding tests, please skip them.
<pre>
mvn clean package -DskipTests=true
</pre>

## Customizing Kustvakt configuration

Copy the default Kustvakt configuration file (e.g. ```full/src/main/resources/kustvakt.conf``` or ```lite/src/main/resources/kustvakt-lite.conf```), to the same  folder as the Kustvakt jar files  (```/target```). Please do not change the name of the configuration file.

### Setting Index Directory

Set krill.indexDir in the configuration file to the location of your Krill index (relative path to the jar). In Kustvakt root directory, there is a sample index, e.g.
<pre>krill.indexDir = ../../sample-index</pre>

### Setting LDAP

Set the location of the LDAP configuration file for Kustvakt full version. The file should contain an admin password to access an LDAP system. Without LDAP, user authentication functions and services cannot be used. Authentication mechanism can be extended by implementing other authentication methods e.g. using a database. 

	ldap.config = path-to-ldap-password

### Setting BasicAuthentication for Testing

For testing, you can use/activate BasicAuthentication, see Spring XML configuration file for testing at ```/full/src/test/resources/test-config.xml```. BasicAuthentication uses a dummy UserDao allowing all users to be authenticated users. You can implement UserDao by connecting it to a user table in a database and checking username and password for authentication. 

	<bean id="basic_auth"
		class="de.ids_mannheim.korap.authentication.BasicAuthentication" />
		
	<util:list id="kustvakt_authproviders"
		value-type="de.ids_mannheim.korap.interfaces.AuthenticationIface">
		<ref bean="basic_auth" />
		...
	</util:list>



## Optional Custom Configuration

### Changing Kustvakt Server Port and Host

	Server.port = 8089
	Server.host = localhost

### Changing Kustvakt Service Base URL

The default base URL is

	kustvakt.base.url=/kustvakt/api/*

### Setting Default Layers

The values of the following properties are foundries. 

	default.layer.partOfSpeech = tt
	default.layer.lemma = tt
	default.layer.orthography = opennlp
	default.layer.dependency = mate
	default.layer.constituent = corenlp


# Running Kustvakt Server
Requires ```kustvakt.conf``` or ```kustvakt-lite.conf``` in the same folder as the jar file. Otherwise assuming sample-index located in the parent directory of the jar file.

Kustvakt full version requires an LDAP configuration file containing an admin password to access an LDAP system. You can still run Kustvakt full version without an LDAP system, but user authentication functions and services cannot be used. Only services for guest/demo user would be available.

<pre>
cd target/
java -jar target/Kustvakt-[lite/full]-[version].jar    
</pre>


# Futher Setup

Advanced setup including database and mailing setting are described in the [wiki](https://github.com/KorAP/Kustvakt/wiki).


# Publication

Diewald, Nils/Hanl, Michael/Margaretha, Eliza/Bingel, Joachim/Kupietz, Marc/Bański, Piotr/Witt, Andreas (2016):
    KorAP Architecture – Diving in the Deep Sea of Corpus Data. In: Calzolari, Nicoletta/Choukri, Khalid/Declerck, Thierry/Goggi, Sara/Grobelnik, Marko/Maegaard, Bente/Mariani, Joseph/Mazo, Helene/Moreno, Asuncion/Odijk, Jan/Piperidis, Stelios (Hrsg.): Proceedings of the Tenth International Conference on Language Resources and Evaluation (LREC 2016), Portorož, Slovenia. Paris: European Language Resources Association (ELRA), 2016. S. 3586-3591.

Bański, Piotr/Diewald, Nils/Hanl, Michael/Kupietz, Marc/Witt, Andreas (2014):
    Access Control by Query Rewriting. The Case of KorAP. In: Proceedings of the Ninth Conference on International Language Resources and Evaluation (LREC’14). European Language Resources Association (ELRA), 2014. S. 3817-3822.


# References

Kupietz, Marc/Lüngen, Harald (2014): Recent Developments in DeReKo. In: Calzolari, Nicoletta et al. (eds.): Proceedings of the Ninth International Conference on Language Resources and Evaluation (LREC'14). Reykjavik: ELRA, 2378-2385.