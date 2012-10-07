/*
Licensed by AT&T under 'Software Development Kit Tools Agreement' 2012.
TERMS AND CONDITIONS FOR USE, REPRODUCTION, AND DISTRIBUTION: http://developer.att.com/sdk_agreement/
Copyright 2012 AT&T Intellectual Property. All rights reserved. 
For more information contact developer.support@att.com http://developer.att.com
*/
package mirw.spherotest;

/** Configuration parameters for this application's account on Speech API. **/
public class SpeechConfig {
	private SpeechConfig() {} // can't instantiate
	
	/** The URL of AT&T Speech API. **/
	static String serviceUrl() {
    	return "https://api.att.com/rest/1/SpeechToText";
	}
	
	/** The URL of AT&T Speech API OAuth service. **/
	static String oauthUrl() {
    	return "https://api.att.com/oauth/token";
	}
	
    /** Unobfuscates the OAuth client_id credential for the application. **/
    static String oauthKey() {
		// TODO: Replace this with code to unobfuscate your OAuth client_id.
		return myUnobfuscate(/* TODO: fill in AT&T OAuth client ID */);
	}

    private static String myUnobfuscate(String string) {
      // TODO Actually unobfuscate.
      return string;
    }

    /** Unobfuscates the OAuth client_secret credential for the application. **/
    static String oauthSecret() {
		// TODO: Replace this with code to unobfuscate your OAuth client_secret.
		return myUnobfuscate(/* TODO: fill in AT&T OAuth secret here */);
    }
   
}
