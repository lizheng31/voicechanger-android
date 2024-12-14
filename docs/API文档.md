音色转换 API 文档
#接口说明
音色转换是基于深度学习技术，输入一段音频，能够自动合成各种不同音色的音频，包括多种男声、女声、童声，同时可以提供情感音色，使合成的语音更加接近人声。

部分开发语言demo如下，其他开发语言请参照文档进行开发，也欢迎热心的开发者到 讯飞开放平台社区 分享你们的demo。
音色转换 demo java语言
音色转换 demo python语言

集成音色转换时，需按照以下要求:

内容	说明
传输方式	ws[s]（为提高安全性，强烈推荐wss）
请求地址	ws(s): //cn-huadong-1.xf-yun.com/v1/private/s5e668773
注：服务器IP不固定，为保证您的接口稳定，请勿通过指定IP的方式调用接口，使用域名方式调用
请求行	GET /v1/private/s5e668773 HTTP/1.1
接口鉴权	签名机制，详情请参照下方鉴权说明
字符编码	UTF-8
响应格式	统一采用JSON格式
开发语言	任意，只要可以向讯飞云服务发起Websocket请求的均可
适用范围	任意操作系统，但因不支持跨域不适用于浏览器
音频格式	mp3, speex, opus
#鉴权说明
在调用业务接口时，请求方需要对请求进行签名，服务端通过签名来校验请求的合法性。

#鉴权方法
通过在请求地址后面加上鉴权相关参数的方式。示例url：

wss://cn-huadong-1.xf-yun.com/v1/private/s5e668773?host=cn-huadong-1.xf-yun.com&date=Wed%2C+07+Dec+2022+07%3A30%3A37+GMT&authorization=YXBpX2tleT0iYTc0NjZkNmY1YTA5OWQzZWQzOTRiM2Y1OTc0NmNlZGIiLCBhbGdvcml0aG09ImhtYWMtc2hhMjU2IiwgaGVhZGVycz0iaG9zdCBkYXRlIHJlcXVlc3QtbGluZSIsIHNpZ25hdHVyZT0iT0dLS3dlbnNDS3NHRVU2Zjl1cWNXelhlcFY1eWJiY0ZScmlMZkR0eDlaYz0i
鉴权参数：

参数	类型	必须	说明	示例
host	string	是	请求主机	cn-huadong-1.xf-yun.com
date	string	是	当前时间戳，RFC1123格式("EEE, dd MMM yyyy HH:mm:ss z")	Wed, 07 Dec 2022 07:39:22 GMT
authorization	string	是	使用base64编码的签名相关信息(签名基于hamc-sha256计算)	参考下方详细生成规则
• date参数生成规则：

date必须是UTC+0或GMT时区，RFC1123格式(Wed, 07 Dec 2022 07:30:37 GMT)。
服务端会对date进行时钟偏移检查，最大允许300秒的偏差，超出偏差的请求都将被拒绝。

• authorization参数生成格式：

1）获取接口密钥APIKey 和 APISecret。
在讯飞开放平台控制台，创建一个应用后打开音色转换页面可以获取，均为32位符串。
2）参数authorization base64编码前（authorization_origin）的格式如下。

api_key="$api_key",algorithm="hmac-sha256",headers="host date request-line",signature="$signature"
其中 api_key 是在控制台获取的APIKey，algorithm 是加密算法（仅支持hmac-sha256），headers 是参与签名的参数（见下方注释）。
signature 是使用加密算法对参与签名的参数签名后并使用base64编码的字符串，详见下方。

注： headers是参与签名的参数，请注意是固定的参数名（"host date request-line"），而非这些参数的值。

3）signature的原始字段(signature_origin)规则如下。

signature原始字段由 host，date，request-line三个参数按照格式拼接成，
拼接的格式为(\n为换行符,’:’后面有一个空格)：

host: $host\ndate: $date\n$request-line
假设

请求url = "wss://cn-huadong-1.xf-yun.com/v1/private/s5e668773"
date = "Wed, 07 Dec 2022 07:39:22 GMT"
那么 signature原始字段(signature_origin)则为：

host: cn-huadong-1.xf-yun.com
date: Wed, 07 Dec 2022 07:39:22 GMT
GET /v1/private/s5e668773 HTTP/1.1 
4）使用hmac-sha256算法结合apiSecret对signature_origin签名，获得签名后的摘要signature_sha。

signature_sha=hmac-sha256(signature_origin,$apiSecret)
其中 apiSecret 是在控制台获取的APISecret

5）使用base64编码对signature_sha进行编码获得最终的signature。

signature=base64(signature_sha)
假设

APISecret = "apisecretXXXXXXXXXXXXXXXXXXXXXXX"	
date = "Wed, 07 Dec 2022 07:39:22 GMT"
则signature为

signature="gXZ6XAYWyGfbDg91WI/IW6E+BsEq8w3NJwBewPdMR/s="
6）根据以上信息拼接authorization base64编码前（authorization_origin）的字符串，示例如下。

api_key="apikeyXXXXXXXXXXXXXXXXXXXXXXXXXX", algorithm="hmac-sha256", headers="host date request-line", signature="/gXZ6XAYWyGfbDg91WI/IW6E+BsEq8w3NJwBewPdMR/s="
注： headers是参与签名的参数，请注意是固定的参数名（"host date request-line"），而非这些参数的值。

7）最后再对authorization_origin进行base64编码获得最终的authorization参数。

authorization = base64(authorization_origin)
示例结果为：
YXBpX2tleT0iYTc0NjZkNmY1YTA5OWQzZWQzOTRiM2Y1OTc0NmNlZGIiLCBhbGdvcml0aG09ImhtYWMtc2hhMjU2IiwgaGVhZGVycz0iaG9zdCBkYXRlIHJlcXVlc3QtbGluZSIsIHNpZ25hdHVyZT0iZ1haNlhBWVd5R2ZiRGc5MVdJL0lXNkUrQnNFcTh3M05Kd0Jld1BkTVIvcz0i
#鉴权结果
如果鉴权失败，则根据不同错误类型返回不同HTTP Code状态码，同时携带错误描述信息，详细错误说明如下：

HTTP Code	说明	错误描述信息	解决方法
401	缺少authorization参数	{"message":"Unauthorized"}	检查是否有authorization参数，详情见authorization参数详细生成规则
401	签名参数解析失败	{“message”:”HMAC signature cannot be verified”}	检查签名的各个参数是否有缺失是否正确，特别确认下复制的api_key是否正确
401	签名校验失败	{“message”:”HMAC signature does not match”}	签名验证失败，可能原因有很多。
1. 检查api_key,api_secret 是否正确。
2.检查计算签名的参数host，date，request-line是否按照协议要求拼接。
3. 检查signature签名的base64长度是否正常(正常44个字节)。
403	时钟偏移校验失败	{“message”:”HMAC signature cannot be verified, a valid date or x-date header is required for HMAC Authentication”}	检查服务器时间是否标准，相差5分钟以上会报此错误
时钟偏移校验失败示例：

HTTP/1.1 403 Forbidden
Date: Mon, 30 Nov 2020 02:34:33 GMT
Content-Length: 116
Content-Type: text/plain; charset=utf-8
{
    "message": "HMAC signature does not match, a valid date or x-date header is required for HMAC Authentication"
}
#请求参数
请求参数示例：

{
	"header": {
		"app_id": "your_appid",
		"status": 0
	},
	"parameter": {
		"xvc": {
			"voiceName": "xiaowanzi",
			"result": {
				"encoding": "lame",
				"sample_rate": 16000,
				"channels": 1,
				"bit_depth": 16,
				"frame_size": 0
			}
		}
	},
	"payload": {
		"input_audio": {
			"encoding": "lame",
			"sample_rate": 16000,
			"channels": 1,
			"bit_depth": 16,
			"status": 0,
			"seq": 0,
			"audio": "SUQzBAAA......",
			"frame_size": 0
		}
	}
}
请求参数说明：

参数名	类型	必传	描述
header	Object	是	协议头部，用于描述平台特性的参数
header.app_id	string	是	在平台申请的appid信息
header.status	int	是	请求状态，可选值为：0-开始、1-继续、2-结束
parameter	Object	是	AI 特性参数，用于控制 AI 引擎特性的开关
parameter.xvc	Object	是	服务别名
parameter.xvc.voiceName	string	否	音色转换的发音人:
chongchong:虫虫，温柔女声（默认）
xiaowanzi:小丸子，可爱女童
chaoge:超哥， 磁性男声
nannan:楠楠，可爱男童
pengfei:小鹏，成熟男声
qige:七哥，磁性男声
xiaosong:宋宝宝，亲切男声
xiaoyaozi:逍遥子，时尚男声
yifei:一菲，甜美女声
chengcheng:程程，时尚女声
xiaoyuan:小媛，时尚女声
parameter.xvc.speed	int	否	音速，最小值:-500, 最大值:500，默认0
parameter.xvc.volume	int	否	音量，最小值:-20, 最大值:20，默认0
parameter.xvc.pitch	int	否	音高，最小值:-500, 最大值:500，默认0
parameter.xvc.vocoder_mode	int	否	模式设置，0:自动模式, 1:强制模式
parameter.xvc.result	Object	是	数据格式预期，用于描述返回结果的编码等相关约束
parameter.xvc.result.encoding	string	否	音频编码，可选值：lame, speex, opus, opus-wb, speex-wb
parameter.xvc.result.sample_rate	int	否	音频采样率，可选值：16000, 8000
parameter.xvc.result.channels	int	否	声道数，可选值：1（默认）
parameter.xvc.result.bit_depth	int	否	位深，可选值：16（默认）
parameter.xvc.result.frame_size	int	否	帧大小，最小值:0（默认）, 最大值:1024
payload	Object	是	数据段，携带请求的数据
payload.input_audio	Object	是	输入数据
payload.input_audio.encoding	string	否	音频编码，可选值：lame, speex, opus, opus-wb, speex-wb
payload.input_audio.sample_rate	int	否	音频采样率，可选值：16000
payload.input_audio.channels	int	否	声道数，可选值：1（默认）, 2
payload.input_audio.bit_depth	int	否	位深，单位bit，可选值：16
payload.input_audio.status	int	是	数据状态，可选值：0-开始, 1-开始, 2-结束
payload.input_audio.seq	int	否	数据序号，最小值:0, 最大值:9999999
payload.input_audio.audio	string	是	音频数据，需base64编码，最小尺寸:0B, 最大尺寸:10485760B
payload.input_audio.frame_size	int	否	帧大小，最小值:0（默认）, 最大值:1024
#返回结果
返回参数示例：

{
	"header": {
		"code": 0,
		"message": "success",
		'sid': 'ase000e48e5@hu184ebb9f44b05c3882',
		'status': 0
	},
	'payload': {
		'result': {
			'audio': 'JiuHMipCAAD......',
			'bit_depth': 16,
			'channels': 1,
			'encoding': 'lame',
			'frame_size': 0,
			'sample_rate': 16000,
			'seq': 1,
			'status': 0
		}
	}
}
返回参数说明:

参数名	类型	描述
header	Object	协议头部，用于描述平台特性的参数
header.code	int	返回码，0表示成功，其它表示异常
header.message	string	错误描述
header.sid	string	本次会话的id
payload	Object	数据段，携带响应的数据
payload.result	Object	输出数据
payload.result.encoding	string	音频编码，可选值：lame, speex, opus, opus-wb, speex-wb
payload.result.sample_rate	int	音频采样率，可选值：16000
payload.result.channels	int	声道数，可选值：1
payload.result.bit_depth	int	位深，单位bit，可选值：16, 8
payload.result.status	int	数据状态，0:开始, 1:开始, 2:结束
payload.result.seq	int	标明数据为第几块，最小值:0, 最大值:9999999
payload.result.audio	string	音频数据，需base64编码