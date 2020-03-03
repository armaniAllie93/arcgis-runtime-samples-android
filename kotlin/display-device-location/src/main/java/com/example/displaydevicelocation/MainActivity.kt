/* Copyright 2016 Esri
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */
package com.esri.arcgisruntime.sample.displaydevicelocation

import android.Manifest
import android.R
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.View
import android.widget.AdapterView
import android.widget.AdapterView.OnItemSelectedListener
import android.widget.Spinner
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.esri.arcgisruntime.mapping.ArcGISMap
import com.esri.arcgisruntime.mapping.Basemap
import com.esri.arcgisruntime.mapping.view.LocationDisplay
import com.esri.arcgisruntime.mapping.view.LocationDisplay.DataSourceStatusChangedListener
import com.esri.arcgisruntime.mapping.view.MapView
import java.util.ArrayList

class MainActivity : AppCompatActivity() {
  private var mMapView: MapView? = null
  private var mLocationDisplay: LocationDisplay? = null
  private var mSpinner: Spinner? = null
  private val requestCode = 2
  var reqPermissions = arrayOf(
    Manifest.permission.ACCESS_FINE_LOCATION,
    Manifest.permission.ACCESS_COARSE_LOCATION
  )

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    setContentView(R.layout.activity_main)
    // Get the Spinner from layout
    mSpinner = findViewById<View>(R.id.spinner) as Spinner
    // Get the MapView from layout and set a map with the BasemapType Imagery
    mMapView =
      findViewById<View>(R.id.mapView) as MapView
    val mMap = ArcGISMap(Basemap.createImagery())
    mMapView!!.map = mMap
    // get the MapView's LocationDisplay
    mLocationDisplay = mMapView!!.locationDisplay
    // Listen to changes in the status of the location data source.
    mLocationDisplay.addDataSourceStatusChangedListener(DataSourceStatusChangedListener { dataSourceStatusChangedEvent ->
      // If LocationDisplay started OK, then continue.
      if (dataSourceStatusChangedEvent.isStarted) return@DataSourceStatusChangedListener
      // No error is reported, then continue.
      if (dataSourceStatusChangedEvent.error == null) return@DataSourceStatusChangedListener
      // If an error is found, handle the failure to start.
      // Check permissions to see if failure may be due to lack of permissions.
      val permissionCheck1 =
        ContextCompat.checkSelfPermission(this@MainActivity, reqPermissions[0]) ==
            PackageManager.PERMISSION_GRANTED
      val permissionCheck2 =
        ContextCompat.checkSelfPermission(this@MainActivity, reqPermissions[1]) ==
            PackageManager.PERMISSION_GRANTED
      if (!(permissionCheck1 && permissionCheck2)) { // If permissions are not already granted, request permission from the user.
        ActivityCompat.requestPermissions(this@MainActivity, reqPermissions, requestCode)
      } else { // Report other unknown failure types to the user - for example, location services may not
        // be enabled on the device.
        val message = String.format(
          "Error in DataSourceStatusChangedListener: %s", dataSourceStatusChangedEvent
            .source.locationDataSource.error.message
        )
        Toast.makeText(this@MainActivity, message, Toast.LENGTH_LONG).show()
        // Update UI to reflect that the location display did not actually start
        mSpinner!!.setSelection(0, true)
      }
    })
    // Populate the list for the Location display options for the spinner's Adapter
    val list: ArrayList<ItemData> = ArrayList<ItemData>()
    list.add(ItemData("Stop", R.drawable.locationdisplaydisabled))
    list.add(ItemData("On", R.drawable.locationdisplayon))
    list.add(ItemData("Re-Center", R.drawable.locationdisplayrecenter))
    list.add(ItemData("Navigation", R.drawable.locationdisplaynavigation))
    list.add(ItemData("Compass", R.drawable.locationdisplayheading))
    val adapter = SpinnerAdapter(this, R.layout.spinner_layout, R.id.txt, list)
    mSpinner!!.adapter = adapter
    mSpinner!!.onItemSelectedListener = object : OnItemSelectedListener {
      override fun onItemSelected(
        parent: AdapterView<*>?,
        view: View,
        position: Int,
        id: Long
      ) {
        when (position) {
          0 ->  // Stop Location Display
            if (mLocationDisplay.isStarted()) mLocationDisplay.stop()
          1 ->  // Start Location Display
            if (!mLocationDisplay.isStarted()) mLocationDisplay.startAsync()
          2 -> {
            // Re-Center MapView on Location
            // AutoPanMode - Default: In this mode, the MapView attempts to keep the location symbol on-screen by
            // re-centering the location symbol when the symbol moves outside a "wander extent". The location symbol
            // may move freely within the wander extent, but as soon as the symbol exits the wander extent, the MapView
            // re-centers the map on the symbol.
            mLocationDisplay.setAutoPanMode(LocationDisplay.AutoPanMode.RECENTER)
            if (!mLocationDisplay.isStarted()) mLocationDisplay.startAsync()
          }
          3 -> {
            // Start Navigation Mode
            // This mode is best suited for in-vehicle navigation.
            mLocationDisplay.setAutoPanMode(LocationDisplay.AutoPanMode.NAVIGATION)
            if (!mLocationDisplay.isStarted()) mLocationDisplay.startAsync()
          }
          4 -> {
            // Start Compass Mode
            // This mode is better suited for waypoint navigation when the user is walking.
            mLocationDisplay.setAutoPanMode(LocationDisplay.AutoPanMode.COMPASS_NAVIGATION)
            if (!mLocationDisplay.isStarted()) mLocationDisplay.startAsync()
          }
        }
      }

      override fun onNothingSelected(parent: AdapterView<*>?) {}
    }
  }

  override fun onRequestPermissionsResult(
    requestCode: Int,
    permissions: Array<String>,
    grantResults: IntArray
  ) { // If request is cancelled, the result arrays are empty.
    if (grantResults.size > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) { // Location permission was granted. This would have been triggered in response to failing to start the
// LocationDisplay, so try starting this again.
      mLocationDisplay!!.startAsync()
    } else { // If permission was denied, show toast to inform user what was chosen. If LocationDisplay is started again,
// request permission UX will be shown again, option should be shown to allow never showing the UX again.
// Alternative would be to disable functionality so request is not shown again.
      Toast.makeText(
        this@MainActivity,
        resources.getString(R.string.location_permission_denied),
        Toast.LENGTH_SHORT
      ).show()
      // Update UI to reflect that the location display did not actually start
      mSpinner!!.setSelection(0, true)
    }
  }

  override fun onPause() {
    super.onPause()
    mMapView!!.pause()
  }

  override fun onResume() {
    super.onResume()
    mMapView!!.resume()
  }

  override fun onDestroy() {
    super.onDestroy()
    mMapView!!.dispose()
  }
}