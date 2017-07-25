//
//  ProfileTableViewController.swift
//  Feedbacker
//
//  Created by Anton Borries on 15.05.17.
//  Copyright © 2017 Anton Borries. All rights reserved.
//

import UIKit
import FirebaseAuth
import FirebaseDatabase

class ProfileTableViewController: UITableViewController {
    
    var personalFeedback : [Feedback]!
    
    
    @IBOutlet var feedbackTable: UITableView!
    
    override func viewDidLoad() {
        super.viewDidLoad()

        
        personalFeedback = [Feedback]()
        loadFeedback()
    }
    
    /**
     Attach Listener to User's Personal Feedback in Firebase
     */
    func loadFeedback(){
        let uid : String = (Auth.auth().currentUser?.uid)!
        let personalFeedbackRef : DatabaseReference = FirebaseHelper.ref.child("users").child(uid).child("feedback")
        personalFeedbackRef.observe(.childAdded, with: { snapshot in
            let id : String = snapshot.key
            FirebaseHelper.ref.child("feedback").child(id).observeSingleEvent(of: .value, with: { (snapshot) in
                let value = snapshot.value as! NSDictionary
                let feedback : Feedback = Feedback.init(dict : value)
                self.personalFeedback.append(feedback)
                //Reverse List to Have Latest at the Top
                self.personalFeedback.reverse()
                self.feedbackTable.reloadData()
            })
        })
        personalFeedbackRef.observe(.childChanged, with: {(snapshot) in
            let id : String = snapshot.key
            FirebaseHelper.ref.child("feedback").child(id).observeSingleEvent(of: .value, with: { (snapshot) in
                let value = snapshot.value as! NSDictionary
                let feedback : Feedback = Feedback.init(dict : value)
                let pos = self.getPosition(id: id)
                self.personalFeedback[pos] = feedback
                self.feedbackTable.reloadData()
            })
        })
        personalFeedbackRef.observe(.childRemoved, with: {snapshot in
            let id : String = snapshot.key
            self.personalFeedback.remove(at: self.getPosition(id: id))
            self.feedbackTable.reloadData()
        })
    }
    
    /**
     Get the Position of a Feedback
     @param id Id of the Feedback
     @return Postion
     */
    func getPosition(id : String) -> Int {
        
        for i in 0..<personalFeedback.count {
            if(personalFeedback?[i].id == id){
                return i
            }
        }
        return -1
    }

    override func didReceiveMemoryWarning() {
        super.didReceiveMemoryWarning()
        // Dispose of any resources that can be recreated.
    }

    // MARK: - Table view data source

    override func numberOfSections(in tableView: UITableView) -> Int {
        return 1
    }

    override func tableView(_ tableView: UITableView, numberOfRowsInSection section: Int) -> Int {
        return personalFeedback.count
    }
    
    /**
     Populate Cell
     */
    override func tableView(_ tableView: UITableView, cellForRowAt indexPath: IndexPath) -> UITableViewCell {
        let cellIdentifier = "FeedbackTableViewCell"
        let cell = tableView.dequeueReusableCell(withIdentifier: cellIdentifier, for: indexPath) as? FeedbackTableViewCell
        
        let feedback : Feedback = personalFeedback[indexPath.row]
        
        let date = Date(timeIntervalSince1970: feedback.getDate())
        let dateFormatter = DateFormatter()
        dateFormatter.locale = Locale.current
        dateFormatter.dateStyle = .short
        dateFormatter.timeStyle = .short
        cell?.dateLabel.text = dateFormatter.string(from: date)
        cell?.cityLabel.text = feedback.city
        cell?.categoryImage.image = nil
        cell?.categoryImage.image = CategoryUtil.getImage(feedback: feedback)
        
        return cell!
    }

    /**
     Give Feedback to Destination which is a FeedbackEditController
     */
    override func prepare(for segue: UIStoryboardSegue, sender: Any?) {
        if segue.identifier == "FeedbackDetail" , let destination = segue.destination as? UINavigationController{
            let cell = sender as? FeedbackTableViewCell
            let indexPath = feedbackTable.indexPath(for: cell!)
            let feedback = personalFeedback[(indexPath?.row)!]
            let targetVC = destination.topViewController as! FeedbackEditController
            targetVC.feedback = feedback
        }
        
    
    }
    
    
}
