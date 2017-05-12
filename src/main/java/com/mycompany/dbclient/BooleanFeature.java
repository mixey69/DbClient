/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.mycompany.dbclient;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author mac
 */
public class BooleanFeature {

    BooleanFeature(ResultSet resultSet, String coloumnName) {
        
        try {
            while (resultSet.next()) {
                String oldIDs = resultSet.getString(coloumnName);
                String[] stringIDs = oldIDs.split(" ");
                for (String ID : stringIDs) {
                    Integer intID = Integer.parseInt(ID);
                }
            }

        } catch (SQLException ex) {
            Logger.getLogger(BooleanFeature.class.getName()).log(Level.SEVERE, null, ex);
        }

    }
    
    BooleanFeature(){}
 
}
