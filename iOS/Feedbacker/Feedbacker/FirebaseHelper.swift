//
//  FirebaseHelper.swift
//  Feedbacker
//
//  Created by Anton Borries on 14/04/2017.
//  Copyright © 2017 Anton Borries. All rights reserved.
//

import Foundation
import FirebaseDatabase
import FirebaseAuth

class FirebaseHelper{
    
    static let ref = Database.database().reference()
    
    /**
        Saves a Feedback
        @param feedback : The Feedback to be saved
    */
    static func saveFeedback(feedback : Feedback){
        let uid : String = (Auth.auth().currentUser?.uid)!
        //Save Feedback in User Section
        ref.child("users").child(uid).child("feedback").child(feedback.id).setValue(feedback.category)
        //If made public save it in Published
        if(feedback.published){
            ref.child("published").child(feedback.id).setValue(feedback.category)
        } else{
            ref.child("published").child(feedback.id).removeValue()
        }
        //Save the Feedback itself
        ref.child("feedback").child(feedback.id).setValue(feedback.getDataAsDict())
    }
    
    /**
     Deletes a Feedback
     @param feedback : The Feedback to be deleted
     */
    static func deleteFeedback(feedback : Feedback){
        let uid : String = (Auth.auth().currentUser?.uid)!
        ref.child("feedback").child(feedback.id).removeValue()
        ref.child("published").child(feedback.id).removeValue()
        ref.child("users").child(uid).child("feedback").child(feedback.id).removeValue()
    }
    
    /**
     Get a new unique Id for a feedback
     @return new unique Id for a feedback
     */
    static func getFeedbackId() -> String {
        return ref.child("feedback").childByAutoId().key
    }
    
}
