package org.openscience.jmolandroid.search;

import java.io.BufferedReader;
import java.io.StringReader;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.util.EntityUtils;
import org.openscience.jmolandroid.FileDialog;
import org.openscience.jmolandroid.R;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.res.Configuration;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;

public class PDBSearchActivity extends Activity {
	
  protected static final String query = "<orgPdbQuery><queryType>org.pdb.query.simple.AdvancedKeywordQuery</queryType><description>Text Search</description><keywords>%s</keywords></orgPdbQuery>";
	protected EditText editText;
	protected PDBResultAdapter adapter;

  @Override
  public void onConfigurationChanged(Configuration newConfig) {
    // Ignore orientation change that restarts activity
    super.onConfigurationChanged(newConfig);
  }
  
	@Override
  public void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.search);
    String text = getIntent().getAction().substring(6);
    editText = (EditText) findViewById(R.id.searchText);
    editText.setText(text);

    ListView resultsList = (ListView) findViewById(R.id.resultsListView);
    adapter = new PDBResultAdapter(this, android.R.layout.simple_list_item_1,
        new ArrayList<PDBResult>());
    resultsList.setAdapter(adapter);

    resultsList.setOnItemClickListener(new OnItemClickListener() {
      @Override
      public void onItemClick(AdapterView<?> parent, View view, int position,
                              long id) {
        final PDBResult result = adapter.getItem(position);

        AlertDialog.Builder builder = new AlertDialog.Builder(
            PDBSearchActivity.this);
        builder.setMessage("Download " + result.getId() + "?").setCancelable(
            false).setPositiveButton("Yes",
            new DialogInterface.OnClickListener() {
              public void onClick(DialogInterface dialog, int id) {
                final Handler handler = new Handler();
                final Downloader dn = new Downloader(PDBSearchActivity.this,
                    handler);
                new AsyncTask<Void, Void, Void>() {
                  @Override
                  protected Void doInBackground(Void... v) {
                    dn.download(result.getId());
                    return null;
                  }

                  @Override
                  protected void onPostExecute(Void v) {
                    if (dn.progressDialog.isShowing()) {
                      dn.progressDialog.dismiss();
                    }

                    getIntent().putExtra(FileDialog.RESULT_PATH,
                        dn.file.getAbsolutePath());
                    setResult(RESULT_OK, getIntent());

                    finish();
                  };
                }.execute((Void[]) null);
              }
            }).setNegativeButton("No", new DialogInterface.OnClickListener() {
          public void onClick(DialogInterface dialog, int id) {
            dialog.cancel();
          }
        });
        AlertDialog alert = builder.create();
        alert.show();
      }
    });

    final Button seachButton = (Button) findViewById(R.id.searchButton);
    seachButton.setOnClickListener(new OnClickListener() {
      @Override
      public void onClick(View v) {
        doSearch();
      }
    });
    
    if (text.length() != 0)
      doSearch();
  }

	protected void doSearch() {
    final ProgressDialog progressDialog = ProgressDialog.show(
        PDBSearchActivity.this, "Searching", "Loading Results", true);
    (new AsyncTask<Void, Void, List<PDBResult>>() {
      protected List<PDBResult> doInBackground(Void... none) {
        try {
          List<String> ids = search(editText.getText().toString());

          if (ids.size() > 1) {
            // Hide keyboard
            InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
            imm.hideSoftInputFromWindow(editText.getWindowToken(), 0);
          }

          return getDescriptions(ids);
        } catch (Exception e) {
          // TODO Auto-generated catch block
          e.printStackTrace();
        }
        return Collections.emptyList();
      }

      protected void onPostExecute(List<PDBResult> data) {
        adapter.clear();
        for (PDBResult result : data)
          adapter.add(result);
        adapter.notifyDataSetChanged();
        progressDialog.dismiss();
      }
    }).execute((Void[]) null);
	}
	
	protected List<String> search(String term) {
		List<String> result = new LinkedList<String>();
		
	    HttpClient httpclient = new DefaultHttpClient();
	    HttpPost httppost = new HttpPost("http://www.pdb.org/pdb/rest/search/");

	    Log.i("JMOL", "Searching for " + term);
	    try {
	        httppost.setHeader("Content-Type", "application/x-www-form-urlencoded");
	        httppost.setEntity(new StringEntity(URLEncoder.encode(String.format(query, term),"UTF-8")));
	        
	        HttpResponse response = httpclient.execute(httppost);
			HttpEntity entity = response.getEntity();
			if (entity != null) {
		    	BufferedReader reader = new BufferedReader(new StringReader(EntityUtils.toString(entity)));
		    	
		    	String id = null;
		    	while ((id = reader.readLine()) != null)
		    		result.add(id);
			}
	    } catch (Exception e) {
	        e.printStackTrace();
	    }
	    
	    Log.i("JMOL", "Found " + result);
	    return result;
	}
	
	protected List<PDBResult> getDescriptions(List<String> ids) throws Exception {
		List<PDBResult> results = new LinkedList<PDBResult>();
		
		String idsAsString = listToCSV(ids.subList(0, Math.min(ids.size(), 50)));
	    Log.i("JMOL", "Getting descriptions for " + idsAsString);

        HttpClient httpclient = new DefaultHttpClient();
	    HttpGet httpget = new HttpGet(String.format("http://www.rcsb.org/pdb/rest/customReport?pdbids=%s&customReportColumns=structureId,structureTitle", idsAsString));
		
        HttpResponse response = httpclient.execute(httpget);
        
		HttpEntity entity = response.getEntity();
		if (entity != null) {
	    	DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
	    	DocumentBuilder db = dbf.newDocumentBuilder();
	    	InputSource is = new InputSource();
	    	is.setCharacterStream(new StringReader(EntityUtils.toString(entity)));
	    	Document doc = db.parse(is);
	    	
	    	NodeList nodes = doc.getElementsByTagName("record");
	    	
	    	for (int i=0; i<nodes.getLength(); i++) {
	    		Element e = (Element)nodes.item(i);
	    		
	    		String id = e.getChildNodes().item(1).getFirstChild().getNodeValue();
	    		String desc = e.getChildNodes().item(3).getFirstChild().getNodeValue();
	    		results.add(new PDBResult(id, desc));
	    	}
		}        
        
		return results;
	}
	
	protected String listToCSV(List<String> values) {
		StringBuilder builder = new StringBuilder();

		for (String value : values) {
			builder.append(value);
			builder.append(',');
		}
		if (builder.length() > 1)
			builder.deleteCharAt(builder.length()-1);
		
		return builder.toString();
	}

}