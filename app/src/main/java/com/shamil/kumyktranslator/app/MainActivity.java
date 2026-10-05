package com.shamil.kumyktranslator.app;

import android.app.Activity;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.os.Bundle;
import android.view.Gravity;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

public class MainActivity extends Activity {

    private boolean ruToKum = true;
    private EditText source;
    private EditText result;
    private TextView direction;
    private TextView stats;
    private TranslationDb db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        db = new TranslationDb(this);

        ScrollView scroll = new ScrollView(this);

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(20), dp(28), dp(20), dp(40));
        scroll.addView(root);

        TextView title = new TextView(this);
        title.setText("Русский ⇄ Кумыкский");
        title.setTextSize(32);
        title.setTypeface(null, 1);
        root.addView(title);

        TextView subtitle = new TextView(this);
        subtitle.setText("Kumyk Translator — нативная Android-версия");
        subtitle.setTextSize(15);
        subtitle.setAlpha(0.65f);
        root.addView(subtitle);

        stats = new TextView(this);
        stats.setPadding(0, dp(18), 0, dp(14));
        root.addView(stats);
        updateStats();

        direction = new TextView(this);
        direction.setText("Русский → Кумыкский");
        direction.setTextSize(20);
        direction.setTypeface(null, 1);
        root.addView(direction);

        Button swap = new Button(this);
        swap.setText("⇄ Поменять языки");
        swap.setAllCaps(false);
        root.addView(swap);

        source = new EditText(this);
        source.setHint("Введите текст");
        source.setTextSize(21);
        source.setMinLines(5);
        source.setGravity(Gravity.TOP);
        root.addView(source);

        Button translate = new Button(this);
        translate.setText("Перевести");
        translate.setAllCaps(false);
        root.addView(translate);

        result = new EditText(this);
        result.setHint("Здесь появится перевод");
        result.setTextSize(21);
        result.setMinLines(5);
        result.setGravity(Gravity.TOP);
        root.addView(result);

        Button copy = new Button(this);
        copy.setText("Копировать результат");
        copy.setAllCaps(false);
        root.addView(copy);

        Button save = new Button(this);
        save.setText("Сохранить как правильный перевод");
        save.setAllCaps(false);
        root.addView(save);

        TextView note = new TextView(this);
        note.setPadding(0, dp(24), 0, 0);
        note.setText(
                "Пока база пустая специально. Непроверенные кумыкские переводы " +
                "мы не добавляем как правильные. Позже подключим корпус и модель."
        );
        note.setAlpha(0.6f);
        root.addView(note);

        swap.setOnClickListener(v -> {
            ruToKum = !ruToKum;

            String a = source.getText().toString();
            String b = result.getText().toString();

            source.setText(b);
            result.setText(a);

            direction.setText(
                    ruToKum
                            ? "Русский → Кумыкский"
                            : "Кумыкский → Русский"
            );
        });

        translate.setOnClickListener(v -> {
            String input = source.getText().toString().trim();

            if (input.isEmpty()) {
                result.setText("");
                return;
            }

            String translated = db.translate(input, ruToKum);

            if (translated == null) {
                result.setText("");

                Toast.makeText(
                        this,
                        "Этой фразы пока нет в локальной базе",
                        Toast.LENGTH_LONG
                ).show();
            } else {
                result.setText(translated);
            }
        });

        copy.setOnClickListener(v -> {
            String text = result.getText().toString().trim();

            if (text.isEmpty()) {
                return;
            }

            ClipboardManager clipboard =
                    (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);

            clipboard.setPrimaryClip(
                    ClipData.newPlainText("translation", text)
            );

            Toast.makeText(
                    this,
                    "Скопировано",
                    Toast.LENGTH_SHORT
            ).show();
        });

        save.setOnClickListener(v -> {
            String src = source.getText().toString().trim();
            String dst = result.getText().toString().trim();

            if (src.isEmpty() || dst.isEmpty()) {
                Toast.makeText(
                        this,
                        "Заполни оба поля",
                        Toast.LENGTH_SHORT
                ).show();

                return;
            }

            String ru = ruToKum ? src : dst;
            String kum = ruToKum ? dst : src;

            db.savePair(ru, kum);

            updateStats();

            Toast.makeText(
                    this,
                    "Перевод сохранён",
                    Toast.LENGTH_SHORT
            ).show();
        });

        setContentView(scroll);
    }

    private void updateStats() {
        stats.setText(
                "Локальная база: " +
                db.count() +
                " подтверждённых пар"
        );
    }

    private int dp(int value) {
        return Math.round(
                value * getResources().getDisplayMetrics().density
        );
    }

    private static class TranslationDb extends SQLiteOpenHelper {

        private static final String DB_NAME = "kumyk.db";
        private static final int DB_VERSION = 1;

        TranslationDb(Context context) {
            super(context, DB_NAME, null, DB_VERSION);
        }

        @Override
        public void onCreate(SQLiteDatabase database) {
            database.execSQL(
                    "CREATE TABLE translations (" +
                    "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                    "ru TEXT NOT NULL," +
                    "kum TEXT NOT NULL," +
                    "ru_norm TEXT NOT NULL," +
                    "kum_norm TEXT NOT NULL," +
                    "UNIQUE(ru_norm, kum_norm)" +
                    ")"
            );
        }

        @Override
        public void onUpgrade(
                SQLiteDatabase database,
                int oldVersion,
                int newVersion
        ) {
        }

        String normalize(String text) {
            return text
                    .trim()
                    .toLowerCase()
                    .replace('ё', 'е')
                    .replaceAll("\\s+", " ")
                    .replaceAll("[.!?…]+$", "");
        }

        void savePair(String ru, String kum) {
            getWritableDatabase().execSQL(
                    "INSERT OR REPLACE INTO translations " +
                    "(ru, kum, ru_norm, kum_norm) " +
                    "VALUES (?, ?, ?, ?)",
                    new Object[]{
                            ru,
                            kum,
                            normalize(ru),
                            normalize(kum)
                    }
            );
        }

        String translate(String text, boolean ruToKum) {

            String srcColumn =
                    ruToKum ? "ru_norm" : "kum_norm";

            String dstColumn =
                    ruToKum ? "kum" : "ru";

            Cursor cursor =
                    getReadableDatabase().query(
                            "translations",
                            new String[]{dstColumn},
                            srcColumn + "=?",
                            new String[]{normalize(text)},
                            null,
                            null,
                            "id DESC",
                            "1"
                    );

            try {
                if (cursor.moveToFirst()) {
                    return cursor.getString(0);
                }

                return null;

            } finally {
                cursor.close();
            }
        }

        int count() {

            Cursor cursor =
                    getReadableDatabase().rawQuery(
                            "SELECT COUNT(*) FROM translations",
                            null
                    );

            try {
                cursor.moveToFirst();
                return cursor.getInt(0);

            } finally {
                cursor.close();
            }
        }
    }
}
