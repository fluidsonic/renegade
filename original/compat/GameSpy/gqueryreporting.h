// GameSpy/gqueryreporting.h - stub (GameSpy SDK not in GPL release)
#pragma once
#ifndef GAMESPY_GQUERYREPORTING_H
#define GAMESPY_GQUERYREPORTING_H
#include "nonport.h"
#include <sys/socket.h>
#include <netinet/in.h>
typedef void* qr_t;
typedef void (*qr_querycallback)(char* outbuf, int maxlen, void* userdata);
inline int  qr_init(qr_t* qrec, const char* ip, int port, const char* gamename,
                    const char* secret_key, qr_querycallback basiccb,
                    qr_querycallback infocb, qr_querycallback rulescb,
                    qr_querycallback playerscb, void* userdata) {
    if (qrec) *qrec = NULL; return 0;
}
inline void qr_think(qr_t qrec) {}
inline void qr_shutdown(qr_t qrec) {}
inline void qr_send_statechanged(qr_t qrec) {}
inline void qr_send_exiting(qr_t qrec) {}
inline void qr_process_queries(qr_t qrec) {}
// Master server list
inline int  get_master_count(void) { return 0; }
inline void clear_master_list(void) {}
inline int  get_sockaddrin(const char* host, int port, struct sockaddr_in* addr, void* unused) {
    return 0;
}
inline void add_master(struct sockaddr_in* addr) {}
#endif
