package com.sky.service.impl;

import com.sky.context.BaseContext;
import com.sky.entity.AddressBook;
import com.sky.mapper.AddressBookMapper;
import com.sky.service.AddressBookService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@Slf4j
public class AddressBookServiceImpl implements AddressBookService {

    @Autowired
    private AddressBookMapper addressBookMapper;

    /**
     * 新增地址（当前用户第一条地址自动设为默认）
     * @param addressBook
     */
    public void saveAddressBook(AddressBook addressBook) {
        addressBook.setUserId(BaseContext.getCurrentId());

        List<AddressBook> list = addressBookMapper.list(AddressBook.builder().userId(addressBook.getUserId()).build());
        if (list == null || list.isEmpty()) {
            addressBook.setIsDefault(1);
        } else {
            addressBook.setIsDefault(0);
        }
        addressBookMapper.insert(addressBook);
    }

    /**
     * 查询当前用户所有地址（默认地址排最前）
     * @return
     */
    public List<AddressBook> list() {
        AddressBook addressBook = AddressBook.builder().userId(BaseContext.getCurrentId()).build();
        return addressBookMapper.list(addressBook);
    }

    /**
     * 根据id查询地址
     * @param id
     * @return
     */
    public AddressBook getById(Long id) {
        return addressBookMapper.getById(id);
    }

    /**
     * 修改地址（user_id 不允许被修改）
     * @param addressBook
     */
    public void update(AddressBook addressBook) {
        addressBookMapper.update(addressBook);
    }

    /**
     * 根据id删除地址
     * @param id
     */
    public void deleteById(Long id) {
        addressBookMapper.deleteById(id);
    }

    /**
     * 设置默认地址：先把该用户所有地址默认标记置0，再设置指定地址为默认
     * @param addressBook
     */
    public void setDefault(AddressBook addressBook) {
        AddressBook ab = AddressBook.builder()
                .userId(BaseContext.getCurrentId())
                .build();
        addressBookMapper.updateIsDefaultByUserId(ab);

        addressBook.setIsDefault(1);
        addressBookMapper.update(addressBook);
    }
}
