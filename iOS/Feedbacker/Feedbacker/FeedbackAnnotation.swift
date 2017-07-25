//
//  FeedbackAnnotation.swift
//  Feedbacker
//
//  Created by Anton Borries on 24.07.17.
//  Copyright © 2017 Anton Borries. All rights reserved.
//

import Foundation
import MapKit.MKAnnotation

/**
 Custom MKAnnotation which also saves a Feedback
 */
class FeedbackAnnotation : NSObject, MKAnnotation {
    
    var feedback : Feedback?
    var coordinate: CLLocationCoordinate2D
    var title: String?
    var subtitle: String?
    
    /**
     Initialize Annotation with Feedback
     @param feedback Feedback to make Annotatin for
     */
    init(feedback : Feedback) {
        self.feedback = feedback
        let coordinate = CLLocationCoordinate2D(latitude: feedback.latitude, longitude: feedback.longitude)
        self.coordinate = coordinate
        
        self.subtitle = feedback.details
        
        let date = Date(timeIntervalSince1970: feedback.getDate())
        let dateFormatter = DateFormatter()
        dateFormatter.locale = Locale.current
        dateFormatter.dateStyle = .short
        dateFormatter.timeStyle = .short
        self.title = dateFormatter.string(from: date)
    }
    
    /**
     Default MKAnnotation init() Function
     */
    init(coordinate: CLLocationCoordinate2D, title: String, subtitle: String) {
        self.coordinate = coordinate
        self.title = title
        self.subtitle = subtitle
    }
    
}
