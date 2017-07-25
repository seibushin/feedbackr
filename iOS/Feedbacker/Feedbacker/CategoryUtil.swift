//
//  CategoryUtil.swift
//  Feedbacker
//
//  Created by Anton Borries on 17.06.17.
//  Copyright © 2017 Anton Borries. All rights reserved.
//

import Foundation
import UIKit.UIImage
import UIKit.UIColor

class CategoryUtil {

    static let POS_GENERAL : String =  "POS_GENERAL"
    static let POS_SIT : String =  "POS_SIT"
    static let POS_TOILET : String =  "POS_TOILET"
    static let POS_DISABILITY : String =  "POS_DISABILITY"
    static let POS_WIFI : String =  "POS_WIFI"
    static let POS_EAT_DRINK : String =  "POS_EAT_DRINK"
    static let POS_VIEW : String =  "POS_VIEW"

    static let NEG_GENERAL : String =  "NEG_GENERAL"
    static let NEG_DARK : String =  "NEG_DARK"
    static let NEG_DIRTY : String =  "NEG_DIRTY"
    static let NEG_PEOPLE : String =  "NEG_PEOPLE"
    static let NEG_WALKING : String =  "NEG_WALKING"
    static let NEG_GRAFFITI : String =  "NEG_GRAFFITI"
    
    static let POS_ARR : [String] = [POS_GENERAL, POS_SIT, POS_TOILET, POS_DISABILITY, POS_WIFI, POS_EAT_DRINK, POS_VIEW]
    static let NEG_ARR : [String] = [NEG_GENERAL, NEG_DARK, NEG_DIRTY, NEG_PEOPLE, NEG_WALKING, NEG_GRAFFITI]
    
    
    /**
     Get Valid Firebase Categories
     @param kind Kind of Feedback
     @return Array of Categories for that kind
     */
    static func getCategories(kind : Bool) -> [String]{
        if(kind){
            return POS_ARR
        }else{
            return NEG_ARR
        }
    }
    
    /**
     Get Localized String Versions of Feedback
     @param kind Kind of Feedback
     @return Array of Categories with Localized String for that kind
     */
    static func getLocalized(kind : Bool) -> [String] {
        if(kind){
            return [NSLocalizedString("cat_pos_general", comment: POS_GENERAL),
                    NSLocalizedString("cat_pos_sit", comment: POS_SIT),
                    NSLocalizedString("cat_pos_toilet", comment: POS_TOILET),
                    NSLocalizedString("cat_pos_disability_access", comment: POS_DISABILITY),
                    NSLocalizedString("cat_pos_wifi", comment: POS_WIFI),
                    NSLocalizedString("cat_pos_eat_drink", comment: POS_EAT_DRINK),
                    NSLocalizedString("cat_pos_nice_view", comment: POS_VIEW)]
        } else{
            return [NSLocalizedString("cat_neg_general", comment: NEG_GENERAL),
                   NSLocalizedString("cat_neg_dark", comment: NEG_DARK),
                   NSLocalizedString("cat_neg_dirty", comment: NEG_DIRTY),
                   NSLocalizedString("cat_neg_people", comment: NEG_PEOPLE),
                   NSLocalizedString("cat_neg_walking_friendly", comment: NEG_WALKING),
                   NSLocalizedString("cat_neg_graffiti", comment: NEG_GRAFFITI)]
        }
    }
    
    /**
     Get Colored Image for Category
     @param feedback Feedback that needs the Image
     @return Colored Image
     */
    static func getImage(feedback : Feedback) -> UIImage {
        let image = getImageResource(feedback: feedback).withRenderingMode(.alwaysTemplate)
        return image.tintWithColor(color: getColor(feedback: feedback))
    }
    
    
    /**
     Get Image for Category
     @param feedback Feedback that needs the Image
     @return Image without Color
     */
    static func getImageResource(feedback : Feedback) -> UIImage {
        switch feedback.category {
        case POS_SIT:
            return #imageLiteral(resourceName: "sit")
        case POS_TOILET:
            return #imageLiteral(resourceName: "wc")
        case POS_DISABILITY:
            return #imageLiteral(resourceName: "access")
        case POS_WIFI:
            return #imageLiteral(resourceName: "wifi")
        case POS_EAT_DRINK:
            return #imageLiteral(resourceName: "restaurant")
        case POS_VIEW:
            return #imageLiteral(resourceName: "view")
        case NEG_DARK:
            return #imageLiteral(resourceName: "bulb")
        case NEG_DIRTY:
            return #imageLiteral(resourceName: "dirty")
        case NEG_PEOPLE:
            return #imageLiteral(resourceName: "group")
        case NEG_WALKING:
            return #imageLiteral(resourceName: "walk")
        case NEG_GRAFFITI:
            return #imageLiteral(resourceName: "grafitti")
        default:
            if feedback.positive {
               return #imageLiteral(resourceName: "up") 
            }
            return #imageLiteral(resourceName: "down")
        }
    }
    
    /**
     Get Color for Kind of Feedback
     @param feedback Feedback that needs the Color
     @return Colore Red for Negative and Green For Positive
     */
    static func getColor(feedback : Feedback) -> UIColor {
        if (feedback.positive) {
            return UIColor(red: 0.293, green: 0.6863, blue: 0.3137, alpha: 1.0)
        }
        return UIColor.red
     }
    
    /**
     Get Default Category
     @param kind Kind of Feedback
     @return Default Category of Positive and Negative Feedback
     */
    static func getDefaultCategory(kind : Bool) -> String{
        return kind ? POS_GENERAL : NEG_GENERAL
    }
    
    /**
     Get Position of Firebase Category to be able to display Spinner with saved Category
     @param kind Kind of Feedback to determine what Array to choose from
     @param category Firebase conform Category
     @return Position in Category Array
     */
    static func getPosition(kind : Bool, category : String) -> Int {
        if kind {
            return POS_ARR.index(of: category) ?? 0
        } else {
            return NEG_ARR.index(of: category) ?? 0
        }
    }
    
    

}



extension UIImage {
    
    /**
     Color a Image
     from: https://gist.github.com/iamjason/a0a92845094f5b210cf8 24.07.2017
     @param color Color that should be used
     @return Colored UIImage that the Function was called from
     */
    func tintWithColor(color:UIColor)->UIImage {
        
        UIGraphicsBeginImageContextWithOptions(self.size, false, UIScreen.main.scale)
        guard let context = UIGraphicsGetCurrentContext() else { return self }
        
        // flip the image
        context.scaleBy(x: 1.0, y: -1.0)
        context.translateBy(x: 0.0, y: -self.size.height)
        
        // multiply blend mode
        context.setBlendMode(.multiply)
        
        let rect = CGRect(x: 0, y: 0, width: self.size.width, height: self.size.height)
        context.clip(to: rect, mask: self.cgImage!)
        color.setFill()
        context.fill(rect)
        
        // create UIImage
        guard let newImage = UIGraphicsGetImageFromCurrentImageContext() else { return self }
        UIGraphicsEndImageContext()
        
        return newImage
        
    }
}
