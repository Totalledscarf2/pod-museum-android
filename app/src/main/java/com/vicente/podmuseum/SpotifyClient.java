package com.vicente.podmuseum;

import android.content.Context;
import android.net.Uri;
import android.util.Base64;
import org.json.*;
import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.security.*;
import java.util.*;

/** Synchronous network boundary: call exclusively from the Activity's worker executor. */
public final class SpotifyClient {
    public static final String REDIRECT="com.vicente.podmuseum://callback";
    public static final String SCOPES="user-read-private user-read-playback-state user-modify-playback-state playlist-read-private playlist-read-collaborative";
    private final TokenVault vault;
    private String clientId,access="",refresh="";
    private long expires=0,blockedUntil=0;
    public SpotifyClient(Context context,String clientId){this.vault=new TokenVault(context);this.clientId=clientId;}
    public void setClientId(String value){clientId=value;}
    public synchronized boolean connected() throws Exception {return !refresh.isEmpty() || vault.get("refresh")!=null;}
    public static String base64(byte[] bytes){return Base64.encodeToString(bytes,Base64.URL_SAFE|Base64.NO_WRAP|Base64.NO_PADDING);}
    private static String random(){byte[] b=new byte[48];new SecureRandom().nextBytes(b);return base64(b);}
    public synchronized Uri authorize() throws Exception {
        if(!clientId.matches("(?i)[a-f0-9]{32}"))throw new Exception("Introduce el Client ID de 32 caracteres en Ajustes.");
        String verifier=random(),state=random();
        vault.put("pending",new JSONObject().put("verifier",verifier).put("state",state).put("created",System.currentTimeMillis()).put("client",clientId).toString());
        String challenge=base64(MessageDigest.getInstance("SHA-256").digest(verifier.getBytes(StandardCharsets.US_ASCII)));
        return Uri.parse("https://accounts.spotify.com/authorize").buildUpon()
            .appendQueryParameter("client_id",clientId).appendQueryParameter("response_type","code")
            .appendQueryParameter("redirect_uri",REDIRECT).appendQueryParameter("scope",SCOPES)
            .appendQueryParameter("code_challenge_method","S256").appendQueryParameter("code_challenge",challenge)
            .appendQueryParameter("state",state).build();
    }
    public synchronized void callback(Uri uri) throws Exception {
        if(!"com.vicente.podmuseum".equals(uri.getScheme())||!"callback".equals(uri.getHost()))throw new Exception("Callback no válido.");
        String pending=vault.get("pending");if(pending==null)throw new Exception("No hay un inicio de sesión pendiente.");
        JSONObject p=new JSONObject(pending);
        if(!Objects.equals(p.getString("state"),uri.getQueryParameter("state")) || System.currentTimeMillis()-p.getLong("created")>900000)
            throw new Exception("El estado de OAuth no coincide o caducó. Vuelve a conectar.");
        vault.remove("pending");
        if(uri.getQueryParameter("error")!=null)throw new Exception("Spotify: "+uri.getQueryParameter("error"));
        String code=uri.getQueryParameter("code");if(code==null)throw new Exception("Spotify no devolvió un código.");
        clientId=p.getString("client");
        store(token(form("grant_type","authorization_code","code",code,"client_id",clientId,"redirect_uri",REDIRECT,"code_verifier",p.getString("verifier"))));
    }
    private static String form(String... pairs) throws Exception {
        StringBuilder b=new StringBuilder();for(int i=0;i<pairs.length;i+=2){if(b.length()>0)b.append('&');b.append(URLEncoder.encode(pairs[i],"UTF-8")).append('=').append(URLEncoder.encode(pairs[i+1],"UTF-8"));}return b.toString();
    }
    private JSONObject token(String body) throws Exception {
        return request("https://accounts.spotify.com/api/token","POST",body,"application/x-www-form-urlencoded",null);
    }
    private void store(JSONObject data) throws Exception {
        access=data.getString("access_token");refresh=data.optString("refresh_token",refresh);
        if(!refresh.isEmpty())vault.put("refresh",refresh);
        expires=System.currentTimeMillis()+data.optLong("expires_in",3600)*1000;
    }
    private synchronized String access(boolean force) throws Exception {
        if(!force&&!access.isEmpty()&&System.currentTimeMillis()<expires-60000)return access;
        if(refresh.isEmpty()){String saved=vault.get("refresh");refresh=saved==null?"":saved;}
        if(refresh.isEmpty())throw new Exception("Conecta tu cuenta en Ajustes → Conectar Spotify.");
        store(token(form("grant_type","refresh_token","client_id",clientId,"refresh_token",refresh)));
        return access;
    }
    public JSONObject api(String path,String method,JSONObject body) throws Exception {
        if(!path.startsWith("/"))throw new IllegalArgumentException("API path");
        if(System.currentTimeMillis()<blockedUntil)throw new Exception("Spotify limita temporalmente las solicitudes. Espera unos segundos.");
        try{return request("https://api.spotify.com/v1"+path,method,body==null?null:body.toString(),"application/json",access(false));}
        catch(ApiError error){if(error.status==401)return request("https://api.spotify.com/v1"+path,method,body==null?null:body.toString(),"application/json",access(true));throw error;}
    }
    public synchronized void logout(){vault.clear();access="";refresh="";expires=0;}
    private JSONObject request(String url,String method,String body,String contentType,String bearer) throws Exception {
        HttpURLConnection c=(HttpURLConnection)new URL(url).openConnection();
        c.setConnectTimeout(15000);c.setReadTimeout(20000);c.setInstanceFollowRedirects(false);c.setRequestMethod(method);
        if(bearer!=null)c.setRequestProperty("Authorization","Bearer "+bearer);
        try{
            if(body!=null){c.setDoOutput(true);c.setRequestProperty("Content-Type",contentType);try(OutputStream out=c.getOutputStream()){out.write(body.getBytes(StandardCharsets.UTF_8));}}
            int status=c.getResponseCode();if(status==204)return new JSONObject();
            InputStream stream=status>=400?c.getErrorStream():c.getInputStream();
            String text="";if(stream!=null)try(InputStream in=stream;ByteArrayOutputStream out=new ByteArrayOutputStream()){byte[] b=new byte[8192];int n;while((n=in.read(b))!=-1)out.write(b,0,n);text=out.toString("UTF-8");}
            JSONObject data;try{data=text.isEmpty()?new JSONObject():new JSONObject(text);}catch(JSONException e){throw new ApiError(status,"Respuesta no válida de Spotify.");}
            if(status>=400){
                JSONObject error=data.optJSONObject("error");String message=error!=null?error.optString("message"):data.optString("error_description",data.optString("error","Error de Spotify"));
                if(status==429){int wait=30;try{wait=Integer.parseInt(c.getHeaderField("Retry-After"));}catch(Exception ignored){}blockedUntil=System.currentTimeMillis()+Math.max(1,wait)*1000L;message="Límite de Spotify. Espera "+wait+" segundos.";}
                if(status==403)message+=". Comprueba Premium, permisos y usuarios autorizados de tu app.";
                if(status==404)message+=". Abre Spotify, inicia una canción y selecciona un dispositivo.";
                throw new ApiError(status,message);
            }
            return data;
        }finally{c.disconnect();}
    }
    public static final class ApiError extends Exception {public final int status;public ApiError(int status,String text){super("Spotify "+status+": "+text);this.status=status;}}
}
