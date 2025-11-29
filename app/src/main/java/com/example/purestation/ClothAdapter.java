package com.example.purestation;

import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.RecyclerView;
import com.bumptech.glide.Glide;
import com.bumptech.glide.load.DataSource;
import com.bumptech.glide.load.engine.GlideException;
import com.bumptech.glide.request.RequestListener;
import com.bumptech.glide.request.target.Target;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.util.ArrayList;
import java.util.List;

public class ClothAdapter extends RecyclerView.Adapter<ClothAdapter.ClothViewHolder> {

    private Context context;
    private List<String> imageUrls;
    private OnItemClickListener listener;

    private boolean isEditMode = false; // 편집 모드 상태

    public interface OnItemClickListener {
        void onItemClick(String imageUrl);
    }

    public ClothAdapter(Context context, List<String> imageUrls, OnItemClickListener listener) {
        this.context = context;
        this.imageUrls = imageUrls;
        this.listener = listener;
    }

    public static class ClothViewHolder extends RecyclerView.ViewHolder {
        public ImageButton clothImg;
        public ImageButton btnDel; // 삭제 버튼

        public ClothViewHolder(View itemView) {
            super(itemView);
            clothImg = itemView.findViewById(R.id.cloth_img);
            btnDel = itemView.findViewById(R.id.btnDel);
        }

        public void bind(final String imageUrl, final OnItemClickListener listener) {
            clothImg.setOnClickListener(v -> listener.onItemClick(imageUrl));
        }
    }

    // 편집 모드 상태 변경
    public void setEditMode(boolean editMode) {
        isEditMode = editMode;
        notifyDataSetChanged(); // 편집 모드 변경 시 아이템 갱신
    }

    @NonNull
    @Override
    public ClothViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.clothes_item, parent, false);
        return new ClothViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ClothViewHolder holder, int position) {
        String imageUrl = imageUrls.get(position);

        Log.d("GlideDebug", "Loading image from URL: " + imageUrl); // 디버그 로그 추가

        // Glide를 사용하여 이미지를 로드하고 ImageView에 설정합니다.
        Glide.with(holder.itemView.getContext())
                .load(imageUrl)
                .listener(new RequestListener<Drawable>() {
                    @Override
                    public boolean onLoadFailed(@Nullable GlideException e, Object model, Target<Drawable> target, boolean isFirstResource) {
                        Log.e("GlideDebug", "Image Load Failed", e);
                        return false;
                    }

                    @Override
                    public boolean onResourceReady(Drawable resource, Object model, Target<Drawable> target, DataSource dataSource, boolean isFirstResource) {
                        Log.d("GlideDebug", "Image Load Successful");
                        return false;
                    }
                })
                .into(holder.clothImg);

        // 편집 모드인 경우 삭제 버튼 띄움
        if (isEditMode) {
            holder.btnDel.setVisibility(View.VISIBLE);
            holder.btnDel.setOnClickListener(v -> {
                // 삭제 확인 대화상자 표시
                deleteClothesConfirm(position);
            });
        } else {
            holder.btnDel.setVisibility(View.GONE);
        }

        // 이미지 클릭 시 Firebase에서 데이터 가져와서 DetailActivity로 전달
        holder.clothImg.setOnClickListener(v -> fetchClothData(imageUrl));
    }

    private void deleteClothesConfirm(int position) {
        new AlertDialog.Builder(context)
                .setTitle("옷 삭제 확인")
                .setMessage("해당 옷을 정말 삭제하시겠습니까?")
                .setPositiveButton("확인", (dialog, which) -> {
                    deleteCloth(position); // 삭제 수행
                })
                .setNegativeButton("취소", null)
                .show();
    }

    private void deleteCloth(int position) {
        String imageUrl = imageUrls.get(position);
        DatabaseReference ref = FirebaseDatabase.getInstance().getReference("Clothes");

        Query query = ref.orderByChild("url").equalTo(imageUrl);
        // 리얼타임 데이터 삭제
        query.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                for (DataSnapshot clothSnapshot : snapshot.getChildren()) {
                    clothSnapshot.getRef().removeValue(); // 데이터 삭제
                }
                
                // 스토리지 사진 파일 삭제
                String fileName = getFileNameFromUrl(imageUrl);
                if (fileName != null) {
                    StorageReference storageRef = FirebaseStorage.getInstance().getReference("Clothes/" + fileName);
                    storageRef.delete().addOnSuccessListener(aVoid -> {
                        Log.d("ClothAdapter", "스토리지 이미지를 삭제하였습니다.");
                    }).addOnFailureListener(e -> {
                        Log.e("ClothAdapter", "스토리지 이미지 삭제에 실패했습니다.", e);
                    });
                } else {
                    Log.e("ClothAdapter", "스토리지 주소를 찾을 수 없습니다.");
                }


                imageUrls.remove(position); // 목록에서 아이템 제거
                notifyItemRemoved(position); // RecyclerView 갱신
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e("ClothAdapter", "옷 삭제에 실패했습니다.", error.toException());
            }
        });
    }

    private String getFileNameFromUrl(String imageUrl) {
        try {
            // url에서 파일명 추출
            return imageUrl.substring(imageUrl.lastIndexOf("Clothes%2F") + "Clothes%2F".length(), imageUrl.indexOf("?"));
        } catch (Exception e) {
            Log.e("ClothAdapter", "url에서 스토리지 파일명을 가져오지 못 했습니다.", e);
            return null;
        }
    }

    @Override
    public int getItemCount() {
        return imageUrls.size();
    }

    // firebase에서 데이터 가져옴
    public void fetchClothData(String url) {
        DatabaseReference clothes = FirebaseDatabase.getInstance().getReference("Clothes");

        clothes.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                for (DataSnapshot clothSnapshot : snapshot.getChildren()) {
                    // 각 옷 데이터에서 URL을 가져옴
                    String storedImageUrl = clothSnapshot.child("url").getValue(String.class);

                    // URL이 일치하는 경우 데이터를 가져옴
                    if (storedImageUrl != null && storedImageUrl.equals(url)) {
                        // 데이터를 받아 DetailActivity로 전달
                        String color = clothSnapshot.child("color").getValue(String.class);
                        String material = clothSnapshot.child("material").getValue(String.class);
                        String season = clothSnapshot.child("season").getValue(String.class);
                        String washing = clothSnapshot.child("washing").getValue(String.class);
                        String caution = clothSnapshot.child("caution").getValue(String.class);

                        // DetailActivity로 데이터 전달
                        Intent intent = new Intent(context, DetailActivty.class);
                        intent.putExtra("imageUrl", url);
                        intent.putExtra("color", color);
                        intent.putExtra("material", material);
                        intent.putExtra("season", season);
                        intent.putExtra("washing", washing);
                        intent.putExtra("caution", caution);
                        context.startActivity(intent);
                        break;
                    } else {
                        Log.e("ClothAdapterDatabase", "일치하는 데이터가 없습니다. 데이터를 넣어주세요.");
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e("ClothFirebase", "Database error: " + error.getMessage());
            }
        });

    }

}

