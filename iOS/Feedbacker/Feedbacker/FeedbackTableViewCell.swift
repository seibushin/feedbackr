//
//  FeedbackTableViewCell.swift
//  Feedbacker
//
//  Created by Anton Borries on 15.05.17.
//  Copyright © 2017 Anton Borries. All rights reserved.
//

import UIKit

/**
 Simple UITableViewCell to display a Feedback in a UITableView
 */
class FeedbackTableViewCell: UITableViewCell {

    @IBOutlet weak var dateLabel: UILabel!
    @IBOutlet weak var cityLabel: UILabel!
    @IBOutlet weak var categoryImage: UIImageView!
    
    
    
    override func awakeFromNib() {
        super.awakeFromNib()
        // Initialization code
    }

    override func setSelected(_ selected: Bool, animated: Bool) {
        super.setSelected(selected, animated: animated)

        // Configure the view for the selected state
    }

}
