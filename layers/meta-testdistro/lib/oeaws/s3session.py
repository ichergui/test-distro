# ex:ts=4:sw=4:sts=4:et
# -*- tab-width: 4; c-basic-offset: 4; indent-tabs-mode: nil -*-
"""
AWS S3 session management using boto3/botocore.

Speeds up S3 fetcher operations by directly calling boto3
APIs.  Can also be used for S3 uploads.

Remember to whitelist any AWS_* environment variables you
may need to use for S3 access during builds.

Copyright (c) 2019, Matthew Madison <matt@madison.systems>
"""

import os
import time
import bb
import boto3
import botocore

class S3Session(object):
    def __init__(self):
        self.s3client = None

    def makeclient(self):
        self.s3client = boto3.Session().client('s3')

    def upload(self, Filename, Bucket, Key):
        if self.s3client is None:
            self.makeclient()
        try:
            self.s3client.upload_file(Bucket=Bucket, Key=Key, Filename=Filename)
        except botocore.exceptions.ClientError as e:
            err = e.repsonse['Error']
            bb.warn("{}/{}: {} {}".format(Bucket, Key, err['Code'], err['Message']))
            return False
        return True

    def download(self, Bucket, Key, Filename, quiet=True):
        if self.s3client is None:
            self.makeclient()
        try:
            info = self.s3client.head_object(Bucket=Bucket, Key=Key)
            self.s3client.download_file(Bucket=Bucket, Key=Key, Filename=Filename)
            if 'LastModified' in info:
                mtime = int(time.mktime(info['LastModified'].timetuple()))
                os.utime(Filename, (mtime, mtime))
        except botocore.exceptions.ClientError as e:
            err = e.response['Error']
            if quiet and err['Code'] == "404":
                bb.debug(2, "not found: {}/{}".format(Bucket, Key))
            else:
                bb.warn("{}/{}: {} {}".format(Bucket, Key, err['Code'], err['Message']))
            return False
        except OSError as e:
            if quiet:
                pass
            bb.warn("os.utime({}): {} (errno {})".format(Filename, e.strerror, e.errno))
            return False
        return True

    def get_object_info(self, Bucket, Key, quiet=True):
        if self.s3client is None:
            self.makeclient()
        try:
            info = self.s3client.head_object(Bucket=Bucket, Key=Key)
        except botocore.exceptions.ClientError as e:
            err = e.response['Error']
            if quiet and err['Code'] == "404":
                bb.debug(2, "not found: {}/{}".format(Bucket, Key))
            else:
                bb.warn("{}/{}: {} {}".format(Bucket, Key, err['Code'], err['Message']))
            return None
        return info
