//
//  Feedback.swift
//  Feedbacker
//
//  Created by Anton Borries on 10/04/2017.
//  Copyright © 2017 Anton Borries. All rights reserved.
//

import Foundation
import CoreLocation

class Feedback {

    var id : String!
    var date : UInt64
    var latitude, longitude : Double
    var positive : Bool
    var published : Bool = false
    var details : String = ""
    var category : String!
    var city : String!
    
    /**
     Called when a new Feedback is created
     @param location Location of the Feedback
     @param date Time of Feedback
     @param positive Shows what kind the feedback is
     @param id Created Id from Firebase
     @param city City Name
     */
    init(location : CLLocationCoordinate2D, date : Date, positive : Bool, id : String, city : String){
        latitude = location.latitude
        longitude = location.longitude
        
        //Android has Additional Milliseconds thats why the Value passed needs to be *1000
        self.date = UInt64(floor(date.timeIntervalSince1970)*1000)
        
        self.id = id
        
        self.positive = positive
        
        self.category = CategoryUtil.getLocalized(kind: positive)[0]
        
        self.city = city
        
    }
    
    /**
     Creates the Feedback after Loading it from Firebase
     @param dict Firebase Response as Dictionary
     */
    init(dict : NSDictionary) {
        latitude = dict["latitude"] as! Double
        longitude = dict["longitude"] as! Double
        
        date = dict["date"] as! UInt64
        
        id = dict["id"] as! String
        positive = dict["positive"] as! Bool
        
        published = dict["published"] as! Bool
        
        details = dict["details"] as? String ?? ""
        
        category = dict["category"] as! String
        city = dict["city"] as? String ?? ""
    }
    
    /**
     Firebase needs a Dictionary to properly save all Data
     */
    func getDataAsDict() -> [String : AnyObject]{
        let dict = ["id" : self.id, "date" : self.date, "latitude":self.latitude,"longitude":self.longitude,"positive":self.positive,"published":self.published, "details":self.details, "category":self.category, "city":self.city] as [String : AnyObject]
        return dict
    }
    
    /**
     Gets called to mark a Feedback as published
     */
    func publish(published : Bool){
        self.published = published
    }
    
    /**
     Needed for multiplatform as Android saves Date with Milliseconds so to use it without /1000
     */
    func getDate() -> Double {
        return Double(date)/1000.0
    }
    
    /**
     Switches Feedback from positve to negative or other way around
     */
    func changeKind(){
        //Positive --> Negative
        if(positive){
            positive = false
        } else{
            //Negative --> Positive
            positive = true
        }
        category = CategoryUtil.getDefaultCategory(kind: positive)
        print(category)
    }
    
}
