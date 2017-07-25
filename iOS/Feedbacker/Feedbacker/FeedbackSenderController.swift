//
//  FirstViewController.swift
//  Feedbacker
//
//  Created by Anton Borries on 06/04/2017.
//  Copyright © 2017 Anton Borries. All rights reserved.
//

import UIKit
import CoreLocation

class FeedbackSenderController: UIViewController, CLLocationManagerDelegate {
    
    let locationManager = CLLocationManager()
    
    var coordinate : CLLocationCoordinate2D!
    var date : Date!
    var cityname : String!
    
    /**
     Initializes the Location Listener
     */
    override func viewDidLoad() {
        super.viewDidLoad()
        //Init Location Manager
        locationManager.requestAlwaysAuthorization()
        locationManager.requestWhenInUseAuthorization()
        
        if CLLocationManager.locationServicesEnabled(){
            locationManager.delegate = self
            locationManager.desiredAccuracy = kCLLocationAccuracyNearestTenMeters
            locationManager.startUpdatingLocation()
        }
    }
    
    override func didReceiveMemoryWarning() {
        super.didReceiveMemoryWarning()
        // Dispose of any resources that can be recreated.
    }
    
    
    /**
     Gets called when Location Updated
     */
    func locationManager(_ manager: CLLocationManager, didUpdateLocations locations: [CLLocation]) {
        let location = locations[0]
        coordinate = location.coordinate
        date = location.timestamp
        getCityName()
    }
    
    /**
     Creates the Feedback and sends it to FeedbackEditViewController
     */
    override func prepare(for segue: UIStoryboardSegue, sender: Any?) {
        var kind : Bool
        //Based on identifier set Kind
        if segue.identifier == "positive" {
            kind = true
        }else{
            kind = false
        }
        let id : String = FirebaseHelper.getFeedbackId()
        
        let feedback = Feedback(location: coordinate, date: date, positive: kind, id: id, city : cityname ?? "")
        
        //Save the initial Feedback
        FirebaseHelper.saveFeedback(feedback: feedback)
        
        let destinationVC = segue.destination as! UINavigationController
        let targetVC = destinationVC.topViewController as! FeedbackEditController
        targetVC.feedback = feedback
    }
    
    /**
     Saves the current City in Field to save in Feedback
     */
    func getCityName() {
        let geoCoder = CLGeocoder()
        let location = CLLocation(latitude: coordinate.latitude, longitude: coordinate.longitude)
        geoCoder.reverseGeocodeLocation(location, completionHandler: {(placemarks, error) in
        var placeMark : CLPlacemark!
        placeMark = placemarks?[0]
            if(placeMark != nil) {
                if let city = placeMark.addressDictionary!["City"] as? NSString {
                    self.cityname = city as String!
                }
            }
        })
    }
    
    
}

