//______________________________________________________________________________
//
// $Id$
// $Author$
//
// created 10/05 by Dmitry Litvintsev (litvinse@fnal.gov)
//
//______________________________________________________________________________

/*
COPYRIGHT STATUS:
  Dec 1st 2001, Fermi National Accelerator Laboratory (FNAL) documents and
  software are sponsored by the U.S. Department of Energy under Contract No.
  DE-AC02-76CH03000. Therefore, the U.S. Government retains a  world-wide
  non-exclusive, royalty-free license to publish or reproduce these documents
  and software for U.S. Government purposes.  All documents and software
  available from this server are protected under the U.S. and Foreign
  Copyright Laws, and FNAL reserves all rights.


 Distribution of the software available from this server is free of
 charge subject to the user following the terms of the Fermitools
 Software Legal Information.

 Redistribution and/or modification of the software shall be accompanied
 by the Fermitools Software Legal Information  (including the copyright
 notice).

 The user is asked to feed back problems, benefits, and/or suggestions
 about the software to the Fermilab Software Providers.


 Neither the name of Fermilab, the  URA, nor the names of the contributors
 may be used to endorse or promote products derived from this software
 without specific prior written permission.



  DISCLAIMER OF LIABILITY (BSD):

  THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
  "AS IS" AND ANY EXPRESS OR IMPLIED  WARRANTIES, INCLUDING, BUT NOT
  LIMITED TO, THE IMPLIED  WARRANTIES OF MERCHANTABILITY AND FITNESS
  FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL FERMILAB,
  OR THE URA, OR THE U.S. DEPARTMENT of ENERGY, OR CONTRIBUTORS BE LIABLE
  FOR  ANY  DIRECT, INDIRECT,  INCIDENTAL, SPECIAL, EXEMPLARY, OR
  CONSEQUENTIAL DAMAGES  (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT
  OF SUBSTITUTE  GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR
  BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY  OF
  LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING
  NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT  OF THE USE OF THIS
  SOFTWARE, EVEN IF ADVISED OF THE  POSSIBILITY OF SUCH DAMAGE.


  Liabilities of the Government:

  This software is provided by URA, independent from its Prime Contract
  with the U.S. Department of Energy. URA is acting independently from
  the Government and in its own private capacity and is not acting on
  behalf of the U.S. Government, nor as its contractor nor its agent.
  Correspondingly, it is understood and agreed that the U.S. Government
  has no connection to this software and in no manner whatsoever shall
  be liable for nor assume any responsibility or obligation for any claim,
  cost, or damages arising out of or resulting from the use of the software
  available from this server.


  Export Control:

  All documents and software available from this server are subject to U.S.
  export control laws.  Anyone downloading information from this server is
  obligated to secure any necessary Government licenses before exporting
  documents or software obtained from this server.
 */

/*
 * SRMRmClient.java
 *
 * Created on January 28, 2003, 2:54 PM
 */

package gov.fnal.srm.util;

import eu.emi.security.authn.x509.X509Credential;
import org.apache.axis.types.URI;

import java.util.Date;

import org.dcache.srm.client.SRMClientV2;
import org.dcache.srm.v2_2.ArrayOfAnyURI;
import org.dcache.srm.v2_2.ISRM;
import org.dcache.srm.v2_2.SrmRmRequest;
import org.dcache.srm.v2_2.SrmRmResponse;
import org.dcache.srm.v2_2.TReturnStatus;
import org.dcache.srm.v2_2.TSURLReturnStatus;
import org.dcache.srm.v2_2.TStatusCode;

public class SRMRmClientV2 extends SRMClient {
    private X509Credential cred;
    private java.net.URI surls[];
    private String surl_strings[];
    private ISRM isrm;

    /** Creates a new instance of SRMGetClient */
    public SRMRmClientV2(Configuration configuration, java.net.URI[] surls, String[] surl_strings) {
        super(configuration);
        this.surls      = surls;
        this.surl_strings=surl_strings;
        try {
            cred = getCredential();
        }
        catch (Exception e) {
            cred = null;
            System.err.println("Couldn't getGssCredential.");
        }
    }

    @Override
    public void connect() throws Exception {
        java.net.URI srmUrl = surls[0];
        isrm = new SRMClientV2(srmUrl,
                               getCredential(),
                               configuration.getRetry_timeout(),
                               configuration.getRetry_num(),
                               doDelegation,
                               fullDelegation,
                               gss_expected_name,
                               configuration.getWebservice_path(),
                               configuration.getX509_user_trusted_certificates(),
                               configuration.getTransport());
    }

    @Override
    public void start() throws Exception {
        if (cred.getCertificate().getNotAfter().before(new Date())) {
            throw new RuntimeException("credentials have expired");
        }
        SrmRmRequest req = new SrmRmRequest();
        URI[] uris = new URI[surls.length];
        for(int i =0; i<surls.length; ++i) {
            uris[i] = new URI(surl_strings[i]);
        }
        req.setArrayOfSURLs(new ArrayOfAnyURI(uris));
        SrmRmResponse resp = isrm.srmRm(req);
        TReturnStatus rs   = resp.getReturnStatus();
        if (rs.getStatusCode() != TStatusCode.SRM_SUCCESS) {
            TStatusCode rc  = rs.getStatusCode();
            StringBuilder sb = new StringBuilder();
            sb.append("Return code: ").append(rc.toString()).append("\n");
            sb.append("Explanation: ").append(rs.getExplanation()).append("\n");
            if(resp.getArrayOfFileStatuses() != null) {
                TSURLReturnStatus[] arrayOfStatuses = resp.getArrayOfFileStatuses().getStatusArray();
                if(arrayOfStatuses != null) {
                    for (int i=0; i<arrayOfStatuses.length; i++) {
                        if(arrayOfStatuses[i] != null ) {
                            sb.append("file#").append(i).append(" : ");
                            if(arrayOfStatuses[i].getSurl() != null) {
                                sb.append(arrayOfStatuses[i].getSurl());
                            }
                            if(arrayOfStatuses[i].getStatus() != null) {
                                sb.append(", ");
                                sb.append(arrayOfStatuses[i].getStatus().getStatusCode());
                                sb.append(", \"");
                                sb.append(arrayOfStatuses[i].getStatus().getExplanation());
                                sb.append( "\"");
                            }
                            sb.append('\n');
                        }
                    }
                }
            }
            System.out.println(sb.toString());
            System.exit(1);
        }
    }


}
