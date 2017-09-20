package de.ids_mannheim.korap.web.utils;

/**
 * @author hanl
 * @date 18/02/2014
 */
public class ShutdownHook extends Thread {

    @Override
    public void run () {
        //        Properties config = ExtensionBeans.getInstance().getConfiguration()
        //                .getMailProperties();

        //                Email e = new SimpleEmail();
        //                try {
        //                    e.setHostName(config.getProperty("mail.host"));
        //                    e.setSmtpPort(587);
        //                    e.setSubject("KorAP Rest service shutdown Notification");
        //                    e.setFrom(config.getProperty("mail.from"));
        //                    e.addTo("hanl@ids-mannheim.de");
        //                    e.setMsg("The KorAP - REST application shut down unexpectedly!!");
        //                    e.send();
        //                } catch (EmailException e1) {
        //                    e1.printStackTrace();
        //                }

    }

}
