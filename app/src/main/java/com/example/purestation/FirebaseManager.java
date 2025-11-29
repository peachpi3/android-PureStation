//package com.example.purestation;
//
//import androidx.annotation.NonNull;
//
//import com.google.firebase.database.DataSnapshot;
//import com.google.firebase.database.DatabaseError;
//import com.google.firebase.database.DatabaseReference;
//import com.google.firebase.database.FirebaseDatabase;
//import com.google.firebase.database.ValueEventListener;
//
//import java.util.ArrayList;
//import java.util.List;
//
//public class FirebaseManager {
//    private DatabaseReference database;
//
//    public FirebaseManager() {
//        database = FirebaseDatabase.getInstance().getReference();
//    }
//
//   // 여러 ID에 해당하는 옷 데이터 가져옴
//    public void getClothingById(List<String> ids, OnClothingDataReceived callback) {//        if (ids == null || ids.isEmpty()) {
//            callback.onDataReceived(null);
//            return;
//        }
//
//        List<ClothingItem> clothingItems = new ArrayList<>();
//
//        for (String id : ids) {
//            database.child("Clothes").child(id)
//                    .addListenerForSingleValueEvent(new ValueEventListener() {
//                        @Override
//                        public void onDataChange(@NonNull DataSnapshot snapshot) {
//                            ClothingItem item = snapshot.getValue(ClothingItem.class);
//                            if (item != null) {
//                                clothingItems.add(item);
//                            }
//
//                            // 모든 데이터를 가져왔으면 콜백 호출
//                            if (clothingItems.size() == ids.size()) {
//                                callback.onDataReceived(clothingItems);
//                            }
//                        }
//
//                        @Override
//                        public void onCancelled(@NonNull DatabaseError error) {
//                            // 에러 처리
//                            callback.onDataReceived(null);
//                        }
//                    });
//        }
//    }
//
//} // FirebaseManager
