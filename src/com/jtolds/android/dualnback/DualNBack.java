package com.jtolds.android.dualnback;

import android.util.Log;
import android.app.Activity;
import android.view.Menu;
import android.view.View;
import android.view.KeyEvent;
import android.widget.TextView;
import android.widget.Button;
import android.view.MenuItem;
import android.os.Bundle;
import android.os.Handler;
import android.os.PowerManager;
import android.graphics.Color;
import android.speech.tts.TextToSpeech;
import android.content.Intent;
import android.content.Context;
import java.util.Random;
import java.util.Locale;

public class DualNBack extends Activity {
    private Random GENERATOR = new Random();
    private long MILLIS_BETWEEN_TICKS = 3000;
    private int DATA_CHECK_CODE_1 = 1;
    private double CHOOSE_MATCH_PROB = .1;

    private boolean m_gameRunning = false;
    private boolean m_auditoryClicked = false;
    private boolean m_visualClicked = false;

    private TextView[] m_positions = new TextView[8];
    private String[] m_letters = new String[8];
    private int[] m_colors = new int[4];
    private int[] m_textColors = new int[4];

    private TextView[] m_positionHistory = new TextView[0];
    private String[] m_letterHistory = new String[0];

    private int m_n = 2;

    private int m_trials = 0;
    private int m_successes = 0;
    private int m_currentColor = 0;

    private TextToSpeech m_tts = null;

    private Handler m_handler = new Handler();
    private Runnable m_tick = new Runnable() {
        public void run() {
            m_handler.postDelayed(this, MILLIS_BETWEEN_TICKS);
            gameTick();
        }
    };

    private PowerManager.WakeLock m_wl = null;

    public void log(String msg) {
        Log.d(getString(R.string.app_name), msg);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        log("onCreate");
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);

        m_wl = ((PowerManager)getSystemService(Context.POWER_SERVICE)
                ).newWakeLock(PowerManager.FULL_WAKE_LOCK, "dualnback");

        m_positions[0] = (TextView)findViewById(R.id.cell_0);
        m_positions[1] = (TextView)findViewById(R.id.cell_1);
        m_positions[2] = (TextView)findViewById(R.id.cell_2);
        m_positions[3] = (TextView)findViewById(R.id.cell_3);
        m_positions[4] = (TextView)findViewById(R.id.cell_8);
        m_positions[5] = (TextView)findViewById(R.id.cell_5);
        m_positions[6] = (TextView)findViewById(R.id.cell_6);
        m_positions[7] = (TextView)findViewById(R.id.cell_7);
//        m_positions[8] = (TextView)findViewById(R.id.cell_4);

        m_letters[0] = "C";
        m_letters[1] = "R";
        m_letters[2] = "T";
        m_letters[3] = "S";
        m_letters[4] = "H";
        m_letters[5] = "J";
        m_letters[6] = "M";
        m_letters[7] = "Q";
//        m_letters[8] = "L";

        m_colors[0] = Color.rgb(0, 255, 0);
        m_colors[1] = Color.rgb(0, 127, 255);
        m_colors[2] = Color.rgb(255, 0, 255);
        m_colors[3] = Color.rgb(255, 127, 0);

        m_textColors[0] = Color.rgb(0, 0, 0);
        m_textColors[1] = Color.rgb(255, 255, 255);
        m_textColors[2] = Color.rgb(255, 255, 255);
        m_textColors[3] = Color.rgb(255, 255, 255);

        ((TextView)findViewById(R.id.n_value)).setText("N = " + m_n);

        ((Button)findViewById(R.id.auditory_button)).setOnClickListener(
                new View.OnClickListener() {
            public void onClick(View v) {
                auditory();
            }
        });

        ((Button)findViewById(R.id.visual_button)).setOnClickListener(
                new View.OnClickListener() {
            public void onClick(View v) {
                visual();
            }
        });

        ((Button)findViewById(R.id.both_button)).setOnClickListener(
                new View.OnClickListener() {
            public void onClick(View v) {
                auditory();
                visual();
            }
        });

        ((Button)findViewById(R.id.smaller_n_button)).setOnClickListener(
                new View.OnClickListener() {
            public void onClick(View v) {
                log("smaller n");
                if(m_n <= 1) return;
                m_n -= 1;
                ((TextView)findViewById(R.id.n_value)).setText("N = " + m_n);
            }
        });

        ((Button)findViewById(R.id.larger_n_button)).setOnClickListener(
                new View.OnClickListener() {
            public void onClick(View v) {
                log("larger n");
                m_n += 1;
                ((TextView)findViewById(R.id.n_value)).setText("N = " + m_n);
            }
        });

        m_tts = null;
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode,
            Intent data) {
        log("onActivityResult " + requestCode + " " + resultCode);
        if(requestCode == DATA_CHECK_CODE_1) {
            if(resultCode == TextToSpeech.Engine.CHECK_VOICE_DATA_PASS) {
                m_tts = new TextToSpeech(this, new TextToSpeech.OnInitListener(){
                    public void onInit(int status) {}
                });
                if(m_tts.isLanguageAvailable(Locale.US) ==
                        TextToSpeech.LANG_AVAILABLE)
                    m_tts.setLanguage(Locale.US);
            }
        }
    }

    @Override
    public void onPause() {
        log("onPause");
        stopGame();
        super.onPause();
    }

    @Override
    public void onDestroy() {
        log("onDestroy");
        disableSound();
        super.onDestroy();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        log("onCreateOptionsMenu");
        getMenuInflater().inflate(R.menu.options_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        log("onOptionsItemSelected " + item.getItemId());
        switch (item.getItemId()) {
            case R.id.start_game:
                startGame();
                return true;
            case R.id.stop_game:
                stopGame();
                return true;
            case R.id.disable_sound:
                disableSound();
                return true;
            case R.id.enable_sound:
                enableSound();
                return true;
            default:
                return super.onContextItemSelected(item);
        }
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        log("onKeyDown " + keyCode);
        switch (keyCode) {
            case KeyEvent.KEYCODE_A:
                auditory();
                return true;
            case KeyEvent.KEYCODE_L:
                visual();
                return true;
            default:
                return super.onKeyDown(keyCode, event);
        }
    }

    public void startGame() {
        log("startGame");
        if(m_gameRunning) return;

        m_positionHistory = new TextView[0];
        m_letterHistory = new String[0];

        m_auditoryClicked = false;
        m_visualClicked = false;

        m_trials = 0;
        m_successes = 0;

        m_gameRunning = true;
        if(m_wl != null) m_wl.acquire();
        m_tick.run();
    }

    public void stopGame() {
        log("stopGame");
        if(!m_gameRunning) return;

        m_handler.removeCallbacks(m_tick);

        turnOffCell();

        ((TextView)findViewById(R.id.status)).setText(getString(
                R.string.status_stopped));
        ((TextView)findViewById(R.id.skill)).setText("");

        if(m_wl != null) m_wl.release();
        m_gameRunning = false;
    }

    public void auditory() {
        log("auditory");
        m_auditoryClicked = true;
    }

    public void visual() {
        log("visual");
        m_visualClicked = true;
    }

    public void gameTick() {
        log("gameTick");
        turnOffCell();


        if(m_positionHistory.length <= m_n) {
            switch (m_n - m_positionHistory.length) {
                case 0:
                    ((TextView)findViewById(R.id.status)).setText("Go!");
                    break;
                case 1:
                    ((TextView)findViewById(R.id.status)).setText("Get set...");
                    break;
                case 2:
                    ((TextView)findViewById(R.id.status)).setText("On your mark...");
                    break;
                default:
                    ((TextView)findViewById(R.id.status)).setText("Wait...");
                    break;
            }
            TextView[] new_positionHistory = new TextView[
                    m_positionHistory.length + 1];
            String[] new_letterHistory = new String[m_letterHistory.length + 1];
            for(int i = 0; i < m_positionHistory.length; i++) {
                new_positionHistory[i] = m_positionHistory[i];
                new_letterHistory[i] = m_letterHistory[i];
            }
            m_positionHistory = new_positionHistory;
            m_letterHistory = new_letterHistory;
        } else {
            String status = "";

            m_trials += 2;

            if(m_letterHistory[0] == m_letterHistory[m_n]) {
                if(m_auditoryClicked) {
                    status += " - GOT LETTER!";
                    m_successes += 1;
                } else {
                    status += " - MISSED LETTER!";
                }
            } else {
                if(m_auditoryClicked) {
                    status += " - NO LETTER!";
                } else {
                    m_successes += 1;
                }
            }

            if(m_positionHistory[0] == m_positionHistory[m_n]) {
                if(m_visualClicked) {
                    status += " - GOT POSITION!";
                    m_successes += 1;
                } else {
                    status += " - MISSED POSITION!";
                }
            } else {
                if(m_visualClicked) {
                    status += " - NO POSITION!";
                } else {
                    m_successes += 1;
                }
            }

            ((TextView)findViewById(R.id.status)).setText(status);
        }

        m_auditoryClicked = false;
        m_visualClicked = false;

        for(int i = m_positionHistory.length - 1; i > 0; i--) {
            m_positionHistory[i] = m_positionHistory[i-1];
            m_letterHistory[i] = m_letterHistory[i-1];
        }

        if(m_positionHistory.length > m_n &&
                GENERATOR.nextDouble() < CHOOSE_MATCH_PROB) {
            m_positionHistory[0] = m_positionHistory[m_n];
        } else {
            m_positionHistory[0] = m_positions[GENERATOR.nextInt(
                    m_positions.length)];
        }
        if(m_positionHistory.length > m_n &&
                GENERATOR.nextDouble() < CHOOSE_MATCH_PROB) {
            m_letterHistory[0] = m_letterHistory[m_n];
        } else {
            m_letterHistory[0] = m_letters[GENERATOR.nextInt(m_letters.length)];
        }

        ((TextView)findViewById(R.id.skill)).setText(
                "" + m_successes + "/" + m_trials);

        turnOnCell();
        if(m_tts != null)
            m_tts.speak(m_letterHistory[0], TextToSpeech.QUEUE_FLUSH, null);
    }

    public void turnOnCell() {
        if(m_positionHistory.length == 0) return;
        TextView cell = m_positionHistory[0];
        cell.setText(m_letterHistory[0]);

        m_currentColor = (m_currentColor + 1) % m_colors.length;
        cell.setBackgroundColor(m_colors[m_currentColor]);
        cell.setTextColor(m_textColors[m_currentColor]);
    }

    public void turnOffCell() {
        if(m_positionHistory.length == 0) return;
        TextView cell = m_positionHistory[0];
        cell.setText("");
        cell.setBackgroundColor(Color.TRANSPARENT);
    }

    public void enableSound() {
        if(m_tts != null) return;
        Intent checkIntent = new Intent();
        checkIntent.setAction(TextToSpeech.Engine.ACTION_CHECK_TTS_DATA);
        startActivityForResult(checkIntent, DATA_CHECK_CODE_1);
    }

    public void disableSound() {
        if(m_tts == null) return;
        m_tts.shutdown();
        m_tts = null;
    }

}
