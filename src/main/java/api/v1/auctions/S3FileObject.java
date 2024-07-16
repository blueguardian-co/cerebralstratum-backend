package api.v1.auctions;

import software.amazon.awssdk.services.s3.model.S3Object;

public class S3FileObject {
    private String objectKey;

    private Long size;

    public S3FileObject() {
    }

    public static S3FileObject from(S3Object s3Object) {
        S3FileObject file = new S3FileObject();
        if (s3Object != null) {
            file.setObjectKey(s3Object.key());
            file.setSize(s3Object.size());
        }
        return file;
    }

    public String getObjectKey() {
        return objectKey;
    }

    public Long getSize() {
        return size;
    }

    public S3FileObject setObjectKey(String objectKey) {
        this.objectKey = objectKey;
        return this;
    }

    public S3FileObject setSize(Long size) {
        this.size = size;
        return this;
    }
}
