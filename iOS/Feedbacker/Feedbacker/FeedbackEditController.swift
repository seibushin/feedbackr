//
//  FeedbackEditViewController.swift
//  Feedbacker
//
//  Created by Anton Borries on 13/04/2017.
//  Copyright © 2017 Anton Borries. All rights reserved.
//

import Foundation
import UIKit
import MapKit
import CoreLocation

class FeedbackEditController: UIViewController, UITextViewDelegate, UIPickerViewDelegate, UIPickerViewDataSource, MKMapViewDelegate {
    var feedback : Feedback!
    
    var annotation : FeedbackAnnotation?
    
    @IBOutlet weak var dateView: UILabel!
    @IBOutlet weak var detailView : UITextView!
    @IBOutlet weak var mapView: MKMapView!
    @IBOutlet weak var categoryPicker: UIPickerView!
    
    @IBOutlet weak var publishSwitch: UISwitch!
    
    /**
     Populate the View
     */
    override func viewDidLoad() {
        super.viewDidLoad()
        
        let date = Date(timeIntervalSince1970: feedback.getDate())
        let dateFormatter = DateFormatter()
        dateFormatter.locale = Locale.current
        dateFormatter.dateStyle = .short
        dateFormatter.timeStyle = .short
        dateView.text = dateFormatter.string(from: date)
        
        publishSwitch.isOn = feedback.published
        
        detailView.delegate = self
        detailView.text = feedback.details
        if(feedback.details == ""){
            detailView.text = NSLocalizedString("describe", comment: "Describe your Feedback...")
            detailView.textColor = UIColor.lightGray
        }
        
        let location = CLLocationCoordinate2D(latitude: feedback.latitude, longitude: feedback.longitude)
        let region = MKCoordinateRegionMakeWithDistance(location, 3000, 3000)
        annotation = FeedbackAnnotation(feedback : feedback)
        
        mapView.setRegion(region, animated: true)
        mapView.addAnnotation(annotation!)
        mapView.isScrollEnabled = false
        
        mapView.delegate = self
        
        categoryPicker.delegate = self
        categoryPicker.dataSource = self
        
        let initialCategoryPosition : Int = CategoryUtil.getPosition(kind: feedback.positive, category: feedback.category)
        categoryPicker.selectRow(initialCategoryPosition, inComponent: 0, animated: false)
    }
    
    /**
     Editing was Cancelled so dismiss the Controller
     */
    @IBAction func cancelEdit(_ sender: Any) {
        self.dismiss(animated: true, completion: nil)
    }
    
    /**
     Save Feedback and Dismiss the Controller
     */
    @IBAction func saveEdit(_ sender: Any) {
        FirebaseHelper.saveFeedback(feedback: feedback)
        self.dismiss(animated: true, completion: nil)
    }
    
    /**
     Close keyboard when Editing ended
     */
    func textView(_ textView: UITextView, shouldChangeTextIn range: NSRange, replacementText text: String) -> Bool {
        switch text {
        case "\n":
            textView.resignFirstResponder()
            return false
        default:
            return true
        }
    }
    
    /**
     UITextView has no Placeholder Function this is a workaround
     */
    func textViewDidBeginEditing(_ textView: UITextView) {
        if (textView.textColor == UIColor.lightGray)  {
            textView.text = nil
            textView.textColor = UIColor.black
        }
    }
    
    /**
     if the User wrote details save them to the Feedback
     If not make it look like a Empty TextView with Placeholder Text
     */
    func textViewDidEndEditing(_ textView: UITextView) {
        if (textView.text.isEmpty) {
            textView.text = NSLocalizedString("describe", comment: "Describe your Feedback...")
            feedback.details = ""
            textView.textColor = UIColor.lightGray
        } else {
            feedback.details = textView.text
        }
    }
    
    /**
     Formats a Number with one Leading zero
     @param int Number to Display
     @return String of a Number between 0 and 99 with Leading Zero
     */
    func lz(number : Int) -> String {
        return String(format : "%02d",number)
    }
    
    /**
     Show a Dialog before Deleting a Feedback where the User has to confirm the deletion
     */
    @IBAction func showDeleteDialog(_ sender: Any) {
        let alert = UIAlertController(title: NSLocalizedString("title_delete", comment: "Delete Feedback?"), message: NSLocalizedString("delete_confirmation", comment: "Are you sure you want to delete the feedback? This can't be undone."), preferredStyle: .actionSheet)
        
        let deleteAction = UIAlertAction(title: NSLocalizedString("delete", comment: "Delete"), style: .destructive, handler: {_ in
            self.deleteFeedback()})
        
        let cancelAction = UIAlertAction(title: NSLocalizedString("cancel", comment: "Cancel"), style: .cancel, handler: nil)
        
        alert.addAction(deleteAction)
        alert.addAction(cancelAction)
        self.present(alert, animated: true, completion: nil)
    }
    
    
    /**
     Show a Dialog before Switching a Feedback where the User has to confirm the switch
     */
    @IBAction func showSwitchDialog(_ sender: Any) {
        let alert = UIAlertController(title: NSLocalizedString("title_switch", comment: "Switch Kind?"), message: NSLocalizedString("switch_warning", comment: "Switching the Feedback will loose the current Category"), preferredStyle: .actionSheet)
        
        let deleteAction = UIAlertAction(title: NSLocalizedString("switch", comment: "Switch"), style: .destructive, handler: {_ in
            self.changeFeedback()})
        
        let cancelAction = UIAlertAction(title: NSLocalizedString("cancel", comment: "Cancel"), style: .cancel, handler: nil)
        
        alert.addAction(deleteAction)
        alert.addAction(cancelAction)
        self.present(alert, animated: true, completion: nil)
    }
    
    /**
     Save the Value of the Switch as Published
     @param sender UISwitch for Published
     */
    @IBAction func publish(_ sender: UISwitch) {
        let value = sender.isOn
        feedback.publish(published : value)
    }
    
    /**
     Delete the Feedback in Firebase and Dismiss the controller
     */
    func deleteFeedback() {
        FirebaseHelper.deleteFeedback(feedback: feedback)
        self.dismiss(animated: false, completion: nil)
    }
    
    /**
     Switch between positive and Negative
     */
    func changeFeedback() {
        feedback.changeKind()
        categoryPicker.reloadAllComponents()
        categoryPicker.selectRow(0, inComponent: 0, animated: false)
        feedback.category = CategoryUtil.getCategories(kind: feedback.positive)[0]
        reloadImage()
    }
    
    
    /**
     Picker should only have one wheel
     */
    func numberOfComponents(in pickerView: UIPickerView) -> Int {
        return 1
    }
    
    /**
     Get Number of Categories from CategoryUtil
     */
    func pickerView(_ pickerView: UIPickerView, numberOfRowsInComponent component: Int) -> Int {
        return CategoryUtil.getLocalized(kind: feedback.positive).count
    }
    
    /**
     Get Category String from Category Util to display
     */
    func pickerView(_ pickerView: UIPickerView, titleForRow row: Int, forComponent component: Int) -> String? {
        return CategoryUtil.getLocalized(kind: feedback.positive)[row]
    }
    
    /**
     Picker Selected a new Item
     Save that in the Feedback and update the Annotation Image
     */
    func pickerView(_ pickerView: UIPickerView, didSelectRow row: Int, inComponent component: Int) {
        feedback.category = CategoryUtil.getCategories(kind: feedback.positive)[row]
        reloadImage()
    }
    
    /**
     Sets up the Annotation with the Image for the Category
     */
    func mapView(_ mapView: MKMapView, viewFor annotation: MKAnnotation) -> MKAnnotationView? {
        
        var annotationView: MKAnnotationView?
        if let dequeuedAnnotationView = mapView.dequeueReusableAnnotationView(withIdentifier: feedback.id) {
            annotationView = dequeuedAnnotationView
            annotationView?.annotation = annotation
        }
        else {
            annotationView = MKAnnotationView(annotation: annotation, reuseIdentifier: feedback.id)
            annotationView?.rightCalloutAccessoryView = UIButton(type: .detailDisclosure)
        }
        
        if let annotationView = annotationView {
            //Don't show a callout when clicked
            annotationView.canShowCallout = false
            let feedback = (annotation as! FeedbackAnnotation).feedback!
            annotationView.image = CategoryUtil.getImage(feedback: feedback)
        }
        
        return annotationView
    }

    /**
     Deletes the old Annotation and adds a new One with the proper Image
     */
    func reloadImage(){
        let oldAnnotation = mapView.annotations
        mapView.removeAnnotations(oldAnnotation)
        mapView.addAnnotation(FeedbackAnnotation.init(feedback: feedback))
    }
    
}
