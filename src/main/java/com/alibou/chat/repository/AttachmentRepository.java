package com.alibou.chat.repository;

import com.alibou.chat.model.Attachment;
import org.springframework.data.repository.CrudRepository;

public interface AttachmentRepository extends CrudRepository<Attachment, String> {
}