package br.com.murilo.praticabluemix;

import android.app.ProgressDialog;
import android.content.Context;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.AppCompatSpinner;
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.ibm.watson.developer_cloud.language_translation.v2.LanguageTranslation;
import com.ibm.watson.developer_cloud.language_translation.v2.model.Translation;
import com.ibm.watson.developer_cloud.language_translation.v2.model.TranslationModel;
import com.ibm.watson.developer_cloud.language_translation.v2.model.TranslationResult;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity implements View.OnClickListener {

    EditText edtFrom, edtTo;
    AppCompatSpinner spnFrom, spnTo;
    Button btnTranslate;

    LanguageTranslation service;
    List<TranslationModel> translationModels;

    public enum Type {FROM, TO}

    ProgressDialog mCurrentProgress;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // finds
        edtFrom = (EditText) findViewById(R.id.edt_from);
        edtTo = (EditText) findViewById(R.id.edt_to);
        spnFrom = (AppCompatSpinner) findViewById(R.id.spn_from);
        spnTo = (AppCompatSpinner) findViewById(R.id.spn_to);
        btnTranslate = (Button) findViewById(R.id.btn_translate);

        // setup service
        service = new LanguageTranslation();
        service.setUsernameAndPassword("959724b7-92e5-4a56-9221-ff5f106adefd", "MWkyqcqMQhBc");

        // listeners
        spnFrom.setOnItemSelectedListener(spinnerFromListener);
        btnTranslate.setOnClickListener(this);
        edtFrom.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                if (actionId == EditorInfo.IME_ACTION_GO) {
                    actionTranslate();
                }
                return false;
            }
        });

        // service
        showLoading();
        new getModelsTask().execute();
    }

    ///////////////////////////////////////////////////////////////
    /////////////////////////// actions////////////////////////////
    ///////////////////////////////////////////////////////////////
    private void actionTranslate() {
        TranslationModel from = (TranslationModel) spnFrom.getSelectedItem();
        TranslationModel to = (TranslationModel) spnTo.getSelectedItem();
        String text = edtFrom.getText().toString();
        if (text.trim().length() > 0) {
            showLoading();
            new TranslationTask().execute(text, from.getSource(), to.getTarget());
        } else {
            Toast.makeText(getApplicationContext(), "Write something to translate.", Toast.LENGTH_SHORT).show();
        }
    }

    ///////////////////////////////////////////////////////////////
    /////////////////////////// dialog ////////////////////////////
    ///////////////////////////////////////////////////////////////
    private void showLoading() {
        mCurrentProgress = ProgressDialog.show(this, null, "Loading...", true);
    }

    private void hideLoading() {
        if (mCurrentProgress != null && mCurrentProgress.isShowing()) {
            mCurrentProgress.dismiss();
        }
    }

    ///////////////////////////////////////////////////////////////
    /////////////////////////// listeners  ////////////////////////
    ///////////////////////////////////////////////////////////////
    AdapterView.OnItemSelectedListener spinnerFromListener = new AdapterView.OnItemSelectedListener() {
        @Override
        public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
            TranslationModel model = (TranslationModel) parent.getItemAtPosition(position);
            List<TranslationModel> distinctTo = new ArrayList<TranslationModel>();
            List<String> targets = new ArrayList<String>();
            for (TranslationModel m : translationModels) {
                if (m.getSource().equals(model.getSource())) {
                    String target = m.getTarget();
                    if (!targets.contains(target)) {
                        targets.add(target);
                        distinctTo.add(m);
                    }
                }
            }

            ArrayAdapter adapterTo = new TranslationModelAdapter(getApplicationContext(),
                    R.layout.custom_spinner,
                    distinctTo,
                    Type.TO
            );
            spnTo.setAdapter(adapterTo);

        }

        @Override
        public void onNothingSelected(AdapterView<?> parent) {

        }
    };

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.btn_translate:
                actionTranslate();
                break;
        }
    }


    ///////////////////////////////////////////////////////////////
    /////////////////////////// async tasks  //////////////////////
    ///////////////////////////////////////////////////////////////
    class TranslationTask extends AsyncTask<String, Void, TranslationResult> {
        protected TranslationResult doInBackground(String... urls) {
            String text = urls[0];
            String from = urls[1];
            String to = urls[2];
            return service.translate(text, from, to);
        }

        protected void onPostExecute(TranslationResult result) {
            String ret = "";
            for (Translation t : result.getTranslations()) {
                ret += t.getTranslation() + "\n";
            }
            if (ret.length() > 0) {
                ret = ret.substring(0, ret.length() - 1);
            }
            edtTo.setText(ret);

            hideLoading();
        }
    }

    class getModelsTask extends AsyncTask<String, Void, List<TranslationModel>> {
        protected List<TranslationModel> doInBackground(String... urls) {
            return service.getModels();
        }

        protected void onPostExecute(List<TranslationModel> translationModels) {

            MainActivity.this.translationModels = translationModels;

            List<TranslationModel> distinctFrom = new ArrayList<>();
            List<String> sources = new ArrayList<>();
            for (TranslationModel t : translationModels) {
                String source = t.getSource();
                if (!sources.contains(source)) {
                    sources.add(source);
                    distinctFrom.add(t);
                }
            }

            TranslationModelAdapter adapterFrom = new TranslationModelAdapter(getApplicationContext(),
                    R.layout.custom_spinner,
                    distinctFrom,
                    Type.FROM
            );
            spnFrom.setAdapter(adapterFrom);

            hideLoading();
        }
    }

    ///////////////////////////////////////////////////////////////
    /////////////////////////// adapter ///////////////////////////
    ///////////////////////////////////////////////////////////////
    public class TranslationModelAdapter extends ArrayAdapter<TranslationModel> {

        Type type;

        public TranslationModelAdapter(Context context, int resource, List<TranslationModel> objects, Type type) {
            super(context, resource, objects);
            this.type = type;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            if (convertView == null) {
                convertView = getLayoutInflater().inflate(R.layout.custom_spinner, parent, false);
            }
            TranslationModel model = getItem(position);
            if (type.equals(Type.FROM)) {
                ((TextView) convertView).setText(model.getSource());
            } else {
                ((TextView) convertView).setText(model.getTarget());
            }
            return convertView;
        }

        @Override
        public View getDropDownView(int position, View convertView, ViewGroup parent) {
            if (convertView == null) {
                convertView = getLayoutInflater().inflate(R.layout.custom_spinner, parent, false);
            }
            TranslationModel model = getItem(position);
            if (type.equals(Type.FROM)) {
                ((TextView) convertView).setText(model.getSource());
            } else {
                ((TextView) convertView).setText(model.getTarget());
            }
            return convertView;
        }
    }

}
