package com.maxsavitsky.documenter.net;

import android.util.Log;

import com.maxsavitsky.documenter.App;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;

import javax.net.ssl.HttpsURLConnection;

public class RequestMaker {

   private static final String TAG = App.TAG + " RequestMaker";

   public static String readFromStream(InputStream inputStream) throws IOException {
      ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
      byte[] buffer = new byte[ 1024 ];
      int len;
      while ( ( len = inputStream.read( buffer ) ) != -1 ) {
         outputStream.write( buffer, 0, len );
      }
      return outputStream.toString();
   }

   public static String readFromConnection(HttpURLConnection connection, byte[] output) throws IOException {
      try {
         connection.connect();
         if(output != null)
            connection.getOutputStream().write( output );

         InputStream inputStream = connection.getInputStream();
         String result = readFromStream( inputStream );
         inputStream.close();
         connection.disconnect();
         return result;
      }catch (IOException e){
         InputStream errorStream = connection.getErrorStream();
         if(errorStream != null){
            String errorResult = readFromStream( errorStream );
            errorStream.close();
            Log.i( TAG, "readFromConnection: error stream available.\nurl=" + connection.getURL().getPath() + "\nresult=" + errorResult );
            return errorResult;
         }else{
            throw e;
         }
      }
   }

   public static String getRequestTo(String sUrl) throws IOException {
      HttpURLConnection connection = resolveHttpProtocol( sUrl );
      connection.setRequestMethod( "GET" );
      connection.connect();
      return readFromConnection( connection, null );
   }

   public static HttpURLConnection resolveHttpProtocol(String url) throws IOException {
      if(url.startsWith( "https://" ))
         return (HttpsURLConnection) new URL( url ).openConnection();
      else
         return (HttpURLConnection) new URL( url ).openConnection();
   }

}
