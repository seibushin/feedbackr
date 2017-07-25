//
//  SecondViewController.swift
//  Feedbacker
//
//  Created by Anton Borries on 06/04/2017.
//  Copyright © 2017 Anton Borries. All rights reserved.
//

import UIKit
import MapKit
import CoreLocation
import FirebaseDatabase
import FirebaseAuth

class MapViewController : UIViewController, MKMapViewDelegate {
    
    @IBOutlet weak var mapView: MKMapView!
    var personalAnnotations : [String : FeedbackAnnotation]?
    var personalFeedback : [String : Feedback]?
    
    var publishedAnnotations : [String : FeedbackAnnotation]?
    var publishedFeedback : [String : Feedback]?
    
    var showPersonal : Bool! = true
    var showPublic : Bool! = false
    @IBOutlet weak var personalBar: UIButton!
    @IBOutlet weak var publicBar: UIButton!
    
    let activatedColor = UIColor(colorLiteralRed: 0.16, green: 0.475, blue: 1.0, alpha: 1.0)
    let deactivatedColor = UIColor(colorLiteralRed: 0.565, green: 0.792, blue: 0.976, alpha: 1.0)
    
    /**
     Set up All relevant Structures
     */
    override func viewDidLoad() {
        super.viewDidLoad()
        
        personalAnnotations = [String : FeedbackAnnotation]()
        personalFeedback = [String : Feedback]()
        
        publishedAnnotations = [String : FeedbackAnnotation]()
        publishedFeedback = [String : Feedback]()
        
        mapView.delegate = self
        personalClick(nil)
        publishedClick(nil)
        
        mapView.showsUserLocation = true
    }
   
    /**
     Shows/Hides personal Feedback on Map
     @param sender Component that called the function
     */
    @IBAction func personalClick(_ sender: Any?) {
        if let _ = sender as? UIButton{
            showPersonal = !showPersonal
        }
        if(showPersonal){
            personalBar.setImage(#imageLiteral(resourceName: "person").withRenderingMode(.alwaysTemplate).tintWithColor(color: activatedColor), for: .normal)
            if(personalFeedback?.isEmpty)!{
                createPersonalMarkers()
            }
        } else{
            personalBar.setImage(#imageLiteral(resourceName: "person").withRenderingMode(.alwaysTemplate).tintWithColor(color: deactivatedColor), for: .normal)
            let uid : String = (Auth.auth().currentUser?.uid)!
            let personalFeedbackRef : DatabaseReference = FirebaseHelper.ref.child("users").child(uid).child("feedback")
            personalFeedbackRef.removeAllObservers()
            
            let annotations = [FeedbackAnnotation](personalAnnotations!.values)
            self.mapView.removeAnnotations(annotations)
            personalAnnotations = [String : FeedbackAnnotation]()
            personalFeedback = [String : Feedback]()
        }
    }
    
    /**
     Shows/Hides published Feedback on Map
     @param sender Component that called the function
     */
    @IBAction func publishedClick(_ sender: Any?) {
        if let _ = sender as? UIButton{
            showPublic = !showPublic
        }
        if(showPublic){
            publicBar.setImage(#imageLiteral(resourceName: "globe").withRenderingMode(.alwaysTemplate).tintWithColor(color: activatedColor), for: .normal)
            if(publishedFeedback?.isEmpty)!{
                self.createPublishedMarkers()
            }
        } else{
            publicBar.setImage(#imageLiteral(resourceName: "globe").withRenderingMode(.alwaysTemplate).tintWithColor(color: deactivatedColor), for: .normal)
            let publishedFeedbackRef : DatabaseReference = FirebaseHelper.ref.child("published")
            publishedFeedbackRef.removeAllObservers()
            
            let annotations = [FeedbackAnnotation](publishedAnnotations!.values)
            self.mapView.removeAnnotations(annotations)
            publishedAnnotations = [String : FeedbackAnnotation]()
            publishedFeedback = [String : Feedback]()
        }
    }
    
    /**
     Creates personal Markes by Attaching Listeners to Firebase User Reference
     */
    func createPersonalMarkers(){
        let uid : String = (Auth.auth().currentUser?.uid)!
        let personalFeedbackRef : DatabaseReference = FirebaseHelper.ref.child("users").child(uid).child("feedback")
        //Observe personal Feedback Ref for new Childs
        personalFeedbackRef.observe(.childAdded, with: { snapshot in
            let id : String = snapshot.key
            FirebaseHelper.ref.child("feedback").child(id).observeSingleEvent(of: .value, with: { (snapshot) in
                let value = snapshot.value as! NSDictionary
                let feedback : Feedback = Feedback.init(dict : value)
                self.personalAnnotation(feedback: feedback)
            })
        })
        //Observe personal Feedback Ref for changes
        personalFeedbackRef.observe(.childChanged, with: { snapshot in
            let id : String = snapshot.key
            FirebaseHelper.ref.child("feedback").child(id).observeSingleEvent(of: .value, with: { (snapshot) in
                let value = snapshot.value as! NSDictionary
                let feedback : Feedback = Feedback.init(dict : value)
                self.personalAnnotation(feedback: feedback)
            })
        })
        //Observe personal Feedback Ref for new deleted Childs
        personalFeedbackRef.observe(.childRemoved, with: {snapshot in
            let id : String = snapshot.key
            self.personalFeedback?.removeValue(forKey: id)
            self.mapView.removeAnnotation((self.personalAnnotations?[id])!)
            self.personalAnnotations?.removeValue(forKey: id)
        })
    }
    
    
    /**
     Puts a Personal Feedback on the Map and saves it in the relevant Data Structures
     @param feedback Feedback to make Annotatin for
     */
    func personalAnnotation(feedback : Feedback){
        let annotation = FeedbackAnnotation(feedback: feedback)
        
        
        if(self.personalFeedback?[feedback.id] != nil){
            self.mapView.removeAnnotation((self.personalAnnotations?[feedback.id])!)
        }
        
        self.mapView.addAnnotation(annotation)
        
        self.personalFeedback?[feedback.id] = feedback
        self.personalAnnotations?[feedback.id] = annotation

    }
    
    
    /**
     Creates personal Markes by Attaching Listeners to Firebase Published Reference
     */
    func createPublishedMarkers(){
        let publishedFeedbackRef : DatabaseReference = FirebaseHelper.ref.child("published")
        publishedFeedbackRef.observe(.childAdded, with: { snapshot in
            let id : String = snapshot.key
            FirebaseHelper.ref.child("feedback").child(id).observeSingleEvent(of: .value, with: { (snapshot) in
                let value = snapshot.value as! NSDictionary
                let feedback : Feedback = Feedback.init(dict : value)
                self.publishedAnnotation(feedback: feedback)
            })
        })
        
        publishedFeedbackRef.observe(.childChanged, with: { snapshot in
            let id : String = snapshot.key
            FirebaseHelper.ref.child("feedback").child(id).observeSingleEvent(of: .value, with: { (snapshot) in
                let value = snapshot.value as! NSDictionary
                let feedback : Feedback = Feedback.init(dict : value)
                self.publishedAnnotation(feedback: feedback)
            })
        })
        
        publishedFeedbackRef.observe(.childRemoved, with: {snapshot in
            let id : String = snapshot.key
            self.publishedFeedback?.removeValue(forKey: id)
            self.mapView.removeAnnotation((self.publishedAnnotations?[id])!)
            self.publishedAnnotations?.removeValue(forKey: id)
        })
    }
    
    /**
     Puts a Published Feedback on the Map and saves it in the relevant Data Structures
     @param feedback Feedback to make Annotatin for
     */
    func publishedAnnotation(feedback : Feedback){
        let annotation = FeedbackAnnotation(feedback: feedback)
        
        if(self.publishedFeedback?[feedback.id] != nil){
            self.mapView.removeAnnotation((self.publishedAnnotations?[feedback.id])!)
        }
        
        self.mapView.addAnnotation(annotation)
        
        self.publishedFeedback?[feedback.id] = feedback
        self.publishedAnnotations?[feedback.id] = annotation
    }
    
    
    /**
     Makes the Annotation the Category Image
     */
    func mapView(_ mapView: MKMapView, viewFor annotation: MKAnnotation) -> MKAnnotationView? {
        guard !(annotation is MKUserLocation) else {
            return nil
        }
        let feedback = (annotation as! FeedbackAnnotation).feedback!
        
        let annotationIdentifier = feedback.id!
    
        
        var annotationView: MKAnnotationView?
        if let dequeuedAnnotationView = mapView.dequeueReusableAnnotationView(withIdentifier: annotationIdentifier) {
            annotationView = dequeuedAnnotationView
            annotationView?.annotation = annotation
        }
        else {
            annotationView = MKAnnotationView(annotation: annotation, reuseIdentifier: annotationIdentifier)
            annotationView?.rightCalloutAccessoryView = UIButton(type: .detailDisclosure)
        }
        
        if let annotationView = annotationView {
            // Only Own Feedback has Button on Callout
            annotationView.canShowCallout = true
            if(personalFeedback?[feedback.id] == nil){
                annotationView.rightCalloutAccessoryView = nil
            }
            annotationView.image = CategoryUtil.getImage(feedback: feedback)
        }
        
        return annotationView
    }
    
    /**
     Handles Click on Callout Button
     */
    func mapView(_ mapView: MKMapView, annotationView view: MKAnnotationView, calloutAccessoryControlTapped control: UIControl) {
        let feedback = (view.annotation as! FeedbackAnnotation).feedback
        //Give Feedback as Sender to open Edit View
        performSegue(withIdentifier: "edit", sender: feedback)
    }
    
    /**
     When Clicked on Callout Button open Feedback Edit View
     */
    override func prepare(for segue: UIStoryboardSegue, sender: Any?) {
        if let identifier = segue.identifier {
            switch identifier {
            case "edit":
                let destination = segue.destination as! UINavigationController
                let targetVC = destination.topViewController as! FeedbackEditController
                targetVC.feedback = sender as! Feedback
                break
            default:
                break
            }
        }
    }

    override func didReceiveMemoryWarning() {
        super.didReceiveMemoryWarning()
        // Dispose of any resources that can be recreated.
    }
    

}

