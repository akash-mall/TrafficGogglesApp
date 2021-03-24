package com.example.trafficgoggles;

import android.app.Activity;
import android.app.Application;
import android.content.Context;
import android.os.Build;
import android.speech.tts.TextToSpeech;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;

import java.util.Hashtable;
import java.util.List;
import java.util.Locale;

public class TTS {
    int speechStatus;
    private TextToSpeech textToSpeech;
    public static Hashtable<String, String[]> Signs;
    static Locale locale;
    static String[] stop,crosswalk,trafficlight,speedlimit;
    private boolean isLanguageAvailable = false;
    static{
        locale = new Locale("hi", "IN");
        stop= new String[3];
        crosswalk = new String[3];
        trafficlight = new String[3];
        speedlimit = new String[3];

        stop[0] = "You encountered a stop sign";
        stop[1] = "vous avez rencontré un panneau d'arrêt";
        stop[2] = "आपके सामने एक स्टॉप साइन है।";
        crosswalk[0] = "There is a crosswalk in front";
        crosswalk[1] = "Il y a un passage pour piétons devant";
        crosswalk[2] = "सामने एक क्रॉसवॉक है";
        trafficlight[0] = "Watch out for the traffic light";
        trafficlight[1] = "Attention aux feux tricolores";
        trafficlight[2] = "ट्रैफिक लाइट पर ध्यान दें";
        speedlimit[0] = "You crossed a speed limit sign";
        speedlimit[1] = "Vous avez franchi un panneau de limitation de vitesse";
        speedlimit[2] = "गति सीमा चिन्ह का पालन करें";
        Signs = new Hashtable<>();
        Signs.put("stop",stop);
        Signs.put("crosswalk", crosswalk);
        Signs.put("trafficlight", trafficlight);
        Signs.put("speedlimit", speedlimit);

    }
    public synchronized void speak(Context context, String Tclass,Locale language) {
        Log.d("MainActivity","Inside TTS");
         textToSpeech = new TextToSpeech(context, new TextToSpeech.OnInitListener() {
            @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
            @Override
            public void onInit(int status) {
                if (status == TextToSpeech.SUCCESS) {
                    int ind=0,ttsLang;

                    if(language==Locale.US || language==Locale.UK){
                        ttsLang = textToSpeech.setLanguage(language);
                        ind = 0;
                        isLanguageAvailable = true;
                    }
                    else if(language==Locale.FRANCE){
                        ind = 1;
                        ttsLang = textToSpeech.setLanguage(language);
                        isLanguageAvailable = true;
                    }
                    else {
                        ttsLang = textToSpeech.isLanguageAvailable(language);
                        switch (ttsLang) {
                            case TextToSpeech.LANG_AVAILABLE:
                                textToSpeech.setLanguage(Locale.forLanguageTag("hi"));
                                isLanguageAvailable = true;
                                break;
                            case TextToSpeech.LANG_COUNTRY_AVAILABLE:
                            case TextToSpeech.LANG_COUNTRY_VAR_AVAILABLE:
                                textToSpeech.setLanguage(language);
                                isLanguageAvailable = true;
                                break;
                        }

                        ind = 2;
                    }
                    if(isLanguageAvailable==false){
                        Toast.makeText(context,"Language Not supported in your phone",Toast.LENGTH_SHORT);
                    }

                    if (ttsLang == TextToSpeech.LANG_MISSING_DATA
                            || ttsLang == TextToSpeech.LANG_NOT_SUPPORTED) {
                        Log.d("MainActivity", "The Language is not supported!");
                    } else {
                        Log.d("MainActivity", "Language Supported.");
                    }
                    Log.d("MainActivity", "Initialization success.");
                    String[] data = Signs.get(Tclass);


                    Log.d("MainActivity","speech msg "+data[ind] + Tclass);
                    speechStatus = textToSpeech.speak(data[ind], TextToSpeech.QUEUE_FLUSH, null);

                    if (speechStatus == TextToSpeech.ERROR) {
                        Log.d("MainActivity", "Error in converting Text to Speech!");
                    }
                } else {
                    // Toast.makeText(getApplicationContext(), "TTS Initialization failed!", Toast.LENGTH_SHORT).show();
                    Log.d("MainActivity", "TTS failed");
                }
            }
        });
    }

}
