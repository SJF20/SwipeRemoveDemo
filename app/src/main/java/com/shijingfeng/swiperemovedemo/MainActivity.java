package com.shijingfeng.swiperemovedemo;

import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.shijingfeng.swiperemovedemo.adapter.SwipeRemoveAdapter;
import com.shijingfeng.swiperemovedemo.bean.SwipeRemoveBean;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    private final String[] IMG_ARRAY = {
            "http://ku.90sjimg.com/element_origin_min_pic/00/54/78/0856d993f3b7b33.jpg",
            "http://ku.90sjimg.com/element_origin_min_pic/00/85/92/8956ea2f6af1c47.jpg",
            "http://ku.90sjimg.com/element_origin_min_pic/00/84/75/8356e7eceb77590.jpg",
            "http://ku.90sjimg.com/element_origin_min_pic/00/48/23/8756d7e93a0fbc3.jpg",
            "http://bpic.588ku.com/element_origin_min_pic/00/74/78/2556e0081cbb679.jpg",
            "http://ku.90sjimg.com/element_origin_min_pic/00/08/70/11569f5a9d89aba.jpg",
            "http://ku.90sjimg.com/element_origin_min_pic/00/62/92/9456dbd074c7000.jpg",
            "http://ku.90sjimg.com/element_origin_min_pic/00/35/03/8656d40ce21aea4.jpg",
            "http://ku.90sjimg.com/element_origin_min_pic/00/55/29/7956d9b5367e1b0.jpg",
            "http://ku.90sjimg.com/element_origin_min_pic/00/20/82/6656cd770f3adab.jpg"
    };

    private RecyclerView rvContent;

    private SwipeRemoveAdapter mAdapter;
    private List<SwipeRemoveBean> mDataList;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        initView();
        initData();
        initAction();
    }

    private void initView() {
        rvContent = findViewById(R.id.rv_content);
    }

    private void initData() {
        mDataList = new ArrayList<>();

        for (int i = 0; i < IMG_ARRAY.length; ++i) {
            mDataList.add(new SwipeRemoveBean(IMG_ARRAY[i], "内容" + i));
        }

        rvContent.setLayoutManager(new LinearLayoutManager(this));
        rvContent.setAdapter(mAdapter = new SwipeRemoveAdapter(this, mDataList));
        rvContent.addItemDecoration(new DividerItemDecoration(this, DividerItemDecoration.VERTICAL));
    }

    private void initAction() {
    }
}
