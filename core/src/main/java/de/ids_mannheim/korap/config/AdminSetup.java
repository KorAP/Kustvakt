package de.ids_mannheim.korap.config;

import java.io.File;
import java.io.FileOutputStream;

import de.ids_mannheim.korap.interfaces.EncryptionIface;

/**
 * Created by hanl on 30.05.16.
 */
@Deprecated
public class AdminSetup {

    private final String token_hash;

    private static AdminSetup setup;


    private AdminSetup (String token_hash) {
        this.token_hash = token_hash;
    }


    public static AdminSetup getInstance () {
        if (setup == null)
            setup = init();
        return setup;
    }


    public String getHash () {
        return this.token_hash;
    }


    private static AdminSetup init () {
        EncryptionIface iface = BeansFactory.getKustvaktContext()
                .getEncryption();
        String token = iface.createToken();
        File store = new File("./admin_token");
        try {
            String hash = iface.secureHash(token);
            AdminSetup setup = new AdminSetup(hash);
            FileOutputStream out = new FileOutputStream(store);
            out.write(token.getBytes());

            out.close();

            store.setReadable(true, true);
            store.setWritable(true, true);
            store.setExecutable(false);
            System.out.println();
            System.out
                    .println("_______________________________________________");
            System.out.println("Token created. Please make note of it!");
            System.out.println("Token: " + token);
            System.out
                    .println("_______________________________________________");
            System.out.println();
            return setup;
        }
        catch (Exception e) {
            throw new RuntimeException("setup failed! ", e);
        }
    }
}
