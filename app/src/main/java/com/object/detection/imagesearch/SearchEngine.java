/*
 * Copyright 2019 Google LLC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.object.detection.imagesearch;

import android.content.Context;
import android.os.AsyncTask;
import android.util.Log;

import com.android.volley.RequestQueue;
import com.android.volley.toolbox.Volley;
import com.google.android.gms.common.util.IOUtils;
import com.google.api.client.extensions.android.json.AndroidJsonFactory;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.services.vision.v1.Vision;
import com.google.api.services.vision.v1.VisionRequestInitializer;
import com.google.api.services.vision.v1.model.BatchAnnotateImagesRequest;
import com.object.detection.R;
import com.object.detection.objectdetection.DetectedObject;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/** A fake search engine to help simulate the complete work flow. */
public class SearchEngine {

  private static final String TAG = "SearchEngine";

  public interface SearchResultListener {
    void onSearchCompleted(DetectedObject object, List<Product> productList);
  }

  private final RequestQueue searchRequestQueue;
  private final ExecutorService requestCreationExecutor;
  Context c;

  public SearchEngine(Context context) {
    c=context;
    searchRequestQueue = Volley.newRequestQueue(context);
    requestCreationExecutor = Executors.newSingleThreadExecutor();
  }
  public void search(DetectedObject object, SearchResultListener listener){
    Log.e(TAG, "search2");
    // Crops the object image out of the full image is expensive, so do it off the UI thread.
    try {
      createRequest(object,c,listener);
    } catch (Exception e) {
      e.printStackTrace();
      Log.e(TAG, "search2: "+e.getMessage());
    }
  }
  private void createRequest(DetectedObject searchingObject, Context c, SearchResultListener listener) throws Exception {
    byte[] objectImageData = searchingObject.getImageData();
    if (objectImageData == null) {
      throw new Exception("Failed to get object image data!");
    }

    // Hooks up with your own product search backend here.
    detectLabels(objectImageData,c,listener,searchingObject);
  }
Boolean isChecking=false;
  public void detectLabels(byte[] objectImageData, Context c, SearchResultListener listener, DetectedObject searchingObject) throws IOException {
    if (isChecking){
      return;
    }
    isChecking=true;
    Vision.Builder visionBuilder = new Vision.Builder(
            new NetHttpTransport(),
            new AndroidJsonFactory(),
            null);

    visionBuilder.setVisionRequestInitializer(
            new VisionRequestInitializer("AIzaSyBWH2E0w-SCXKclAKyC05ds14wG2EPa0jU"));


    visionBuilder.setApplicationName("tadicha-2f7bd");
    Vision vision = visionBuilder.build();
// Create new thread
    new MyAsyncTask(objectImageData,c,listener,searchingObject,vision).execute();



  }
  private class MyAsyncTask extends AsyncTask<Void, Void, List<Product>> {
    byte[] objectImageData;
    Context c;
    SearchResultListener listener;
    DetectedObject searchingObject;
    Vision vision;
    List<Product> productList = new ArrayList<>();

    public MyAsyncTask(byte[] objectImageData, Context c, SearchResultListener listener, DetectedObject searchingObject, Vision vision) {
      this.objectImageData = objectImageData;
      this.c = c;
      this.listener = listener;
      this.searchingObject = searchingObject;
      this.vision = vision;
    }

    @Override
    protected List<Product> doInBackground(Void... params) {

      try {
        List<com.google.api.services.vision.v1.model.AnnotateImageRequest> requests2 = new ArrayList<>();
        InputStream inputStream =
                c.getResources().openRawResource(R.raw.photo);
        byte[] photoData = IOUtils.toByteArray(inputStream);
        inputStream.close();

        com.google.api.services.vision.v1.model.Image inputImage = new com.google.api.services.vision.v1.model.Image();
        inputImage.encodeContent(objectImageData);

        com.google.api.services.vision.v1.model.Feature desiredFeature = new com.google.api.services.vision.v1.model.Feature();
        desiredFeature.setType("LABEL_DETECTION");
        com.google.api.services.vision.v1.model.AnnotateImageRequest request2 = new
                com.google.api.services.vision.v1.model.AnnotateImageRequest();
        request2.setFeatures(Arrays.asList(desiredFeature)).setImage(inputImage);
        requests2.add(request2);

        BatchAnnotateImagesRequest batchRequest =
                new BatchAnnotateImagesRequest();

        batchRequest.setRequests(requests2);
        com.google.api.services.vision.v1.model.BatchAnnotateImagesResponse batchResponse =
                vision.images().annotate(batchRequest).execute();
        com.google.api.services.vision.v1.model.TextAnnotation text = batchResponse.getResponses()
                .get(0).getFullTextAnnotation();
        for (int i = 0; i < (new JSONObject(batchResponse.getResponses().get(0).toString())).getJSONArray("labelAnnotations").length();i++){
          String name=new JSONObject(batchResponse.getResponses().get(0).toString()).getJSONArray("labelAnnotations").getJSONObject(i).getString("description");
          productList.add(
                  new Product(/* imageUrl= */ "", name, "-- " ));

        }
        isChecking=false;
        Log.e(TAG, "TextAnnotation: "+ batchResponse.getResponses().get(0).toString() );
        Log.e(TAG, "TextAnnotation: "+ new JSONObject(batchResponse.getResponses().get(0).toString()).getJSONArray("labelAnnotations").getJSONObject(0).getString("description"));
        Log.e(TAG, "TextAnnotation: "+ batchResponse.toString());

      } catch (IOException e) {
        isChecking=false;
        e.printStackTrace();
        Log.e(TAG, "run: "+e.getMessage() );
      } catch (JSONException e) {
        e.printStackTrace();
        Log.e(TAG, "run: "+e.getMessage() );
      }
      return productList;
    }

    @Override
    protected void onPostExecute(List<Product> result) {
      if(listener!=null){
      listener.onSearchCompleted(searchingObject, result);
    }else{
      Log.e(TAG, "run: null" );
    }
    }

    @Override
    protected void onPreExecute() {}

    @Override
    protected void onProgressUpdate(Void... values) {}
  }
  public void shutdown() {
    searchRequestQueue.cancelAll(TAG);
    requestCreationExecutor.shutdown();
  }
}
