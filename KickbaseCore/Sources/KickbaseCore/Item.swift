//
//  Item.swift
//  Kickbasehelper
//
//  Created by Marco Corro on 27.08.25.
//

#if !SKIP
    import Foundation
    import SwiftData

    @Model
    public final class Item {
        public var timestamp: Date

        public init(timestamp: Date) {
            self.timestamp = timestamp
        }
    }
#endif
