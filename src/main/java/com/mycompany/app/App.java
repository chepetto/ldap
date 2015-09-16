package com.mycompany.app;

import com.unboundid.ldap.sdk.*;
import com.unboundid.util.ssl.SSLUtil;
import com.unboundid.util.ssl.TrustAllTrustManager;

import javax.net.SocketFactory;
import java.security.GeneralSecurityException;
import java.util.ArrayList;
import java.util.List;

/**
 * Hello world!
 *
 */
public class App 
{

    public static final String HOSTNAME = "addcd-105.d.ethz.ch";
    public static final int PORT = 636;


    public static void main( String[] args ) throws EntrySourceException {

        // Use no key manager, and trust all certificates. This would not be used
        // in non-trivial code.
        SSLUtil sslUtil = new SSLUtil(null,new TrustAllTrustManager());

        SocketFactory socketFactory;
        LDAPConnection ldapConnection = null;
        try {

            // Create the socket factory that will be used to make a secure
            // connection to the server.
            socketFactory = sslUtil.createSSLSocketFactory();
            ldapConnection = new LDAPConnection(socketFactory,HOSTNAME,PORT);

        } catch(LDAPException ldapException) {

            ldapException.printStackTrace();
            System.exit(ldapException.getResultCode().intValue());

        } catch(GeneralSecurityException exception) {

            exception.printStackTrace();
            System.exit(1);

        }

        try {

            String ldapAccountDn = "cn=myusername,ou=users,ou=id,ou=hosting,dc=d,dc=ethz,dc=ch";
            String password = "mypassword";


            String userLoggingIn = "someuser";
            String userLoggingInDn = String.format("cn=%s,ou=users,ou=id,ou=hosting,dc=d,dc=ethz,dc=ch", userLoggingIn);

            long maxResponseTimeMillis = 1000;

            BindRequest bindRequest = new SimpleBindRequest(ldapAccountDn, password);
            bindRequest.setResponseTimeoutMillis(maxResponseTimeMillis);
            BindResult bindResult = ldapConnection.bind(bindRequest);

            if(bindResult.getResultCode()==null || !"success".equals(bindResult.getResultCode().getName())) {
                throw new IllegalArgumentException("bind failed");
            }

            Entry userEntry = ldapConnection.getEntry(userLoggingInDn);

            // resolve groups
            List<Entry> groups = new ArrayList<Entry>();
            String[] memberOfValues = userEntry.getAttributeValues("memberOf");
            if (memberOfValues != null) {
                DNEntrySource entrySource = new DNEntrySource(ldapConnection, memberOfValues);
                while (true) {
                    Entry groupEntry = entrySource.nextEntry();
                    if (groupEntry == null) {
                        break;
                    }

                    System.out.println(groupEntry.getAttributeValue("name"));
                    groups.add(groupEntry);
                }
            }

            System.out.println(groups);

            ldapConnection.close();
            System.out.println(bindResult);

        } catch(LDAPException ldapException) {

            ldapConnection.close();
            ldapException.printStackTrace();
            System.exit(ldapException.getResultCode().intValue());

        }

    }
}
