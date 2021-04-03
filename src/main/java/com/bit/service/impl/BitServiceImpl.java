package com.bit.service.impl;

import com.bit.anno.Service;
import com.bit.service.BitService;

@Service("BitService")
public class BitServiceImpl implements BitService {
    @Override
    public String query(String name, String age) {
        return "hello name="+name+",age="+age;
    }
}
