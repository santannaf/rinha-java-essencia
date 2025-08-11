import http from 'k6/http';
import {check, sleep, group} from 'k6';

export const options = {
    stages: [
        {duration: '40s', target: 600},
        {duration: '15s', target: 600},
        {duration: '5s', target: 0},
    ],
    gracefulRampDown: '10s',
    teardownTimeout: '5s',

    discardResponseBodies: true,
    thresholds: {
        http_req_duration: ['p(95)<200', 'p(99)<300'],
        http_req_failed: ['rate<0.01'],
    },
    summaryTrendStats: ['avg', 'min', 'med', 'max', 'p(90)', 'p(95)', 'p(99)'],
};

const BASE_URL = 'http://localhost:8080';

export default function () {
    group("POST /payments", function () {
        const uuid = crypto.randomUUID();
        // const payload = JSON.stringify({
        //     correlationId: `test-id-${__VU % 100}`, // Máximo 100 IDs distintos
        //     amount: (Math.random() * 1000).toFixed(2),
        //     requestedAt: new Date().toISOString(),
        //     type: Math.random() < 0.5 ? "default" : "fallback"
        // });

        const payload = JSON.stringify({
            correlationId: uuid,
            amount: 19.90
        });

        const headers = {'Content-Type': 'application/json'};

        // console.log(JSON.stringify(payload));

        const res = http.post(`${BASE_URL}/payments`, payload, {headers});

        check(res, {
            'POST status is 202': (r) => r.status === 202
        });
    });

    sleep(Math.random() * 0.3); // simula pequenas pausas
}

//
// import http from 'k6/http';
// import {check, sleep, group} from 'k6';
// import {Counter} from 'k6/metrics';
//
// export const options = {
//     stages: [
//         {duration: '40s', target: 600},
//         {duration: '15s', target: 600},
//         {duration: '5s', target: 0},
//     ],
//     gracefulStop: '5s',
//     discardResponseBodies: true,
//     thresholds: {
//         http_req_duration: ['p(95)<200', 'p(99)<300'],
//         http_req_failed: ['rate<0.01'],
//     },
//     summaryTrendStats: ['avg', 'min', 'med', 'max', 'p(90)', 'p(95)', 'p(99)'],
// };
//
// const BASE_URL = 'http://localhost:8080';
//
// // --- contadores por status ---
// const status_total = new Counter('status_total');
// const status_202 = new Counter('status_202');
// const status_503 = new Counter('status_503');
// const status_405 = new Counter('status_405');
// const status_400 = new Counter('status_400');
// const status_411 = new Counter('status_411');
// const status_413 = new Counter('status_413');
// const status_500 = new Counter('status_500');
// const status_other = new Counter('status_other');
//
// function recordStatus(s) {
//     status_total.add(1);
//     switch (s) {
//         case 202:
//             status_202.add(1);
//             break;
//         case 503:
//             status_503.add(1);
//             break;
//         case 405:
//             status_405.add(1);
//             break;
//         case 400:
//             status_400.add(1);
//             break;
//         case 411:
//             status_411.add(1);
//             break;
//         case 413:
//             status_413.add(1);
//             break;
//         case 500:
//             status_500.add(1);
//             break;
//         default:
//             status_other.add(1);
//             break;
//     }
// }
//
// function uuidv4() {
//     return 'xxxxxxxx-xxxx-4xxx-yxxx-xxxxxxxxxxxx'.replace(/[xy]/g, c => {
//         const r = (Math.random() * 16) | 0;
//         const v = c === 'x' ? r : (r & 0x3) | 0x8;
//         return v.toString(16);
//     });
// }
//
// export default function () {
//     group('POST /payments', function () {
//         const payload = JSON.stringify({
//             correlationId: uuidv4(),
//             amount: +(Math.random() * 5000).toFixed(2),
//         });
//         const res = http.post(`${BASE_URL}/payments`, payload, {
//             headers: {'Content-Type': 'application/json'},
//         });
//         recordStatus(res.status);
//         check(res, {'POST status is 202': (r) => r.status === 202});
//     });
//
//     sleep(Math.random() * 0.2);
// }
//
// // ---------- resumo bonito + unidades ----------
// export function handleSummary(data) {
//     const getCount = (name) => data.metrics[name]?.values?.count || 0;
//
//     // tabela de status
//     const total = getCount('status_total') || 1;
//     const rows = [
//         ['202', getCount('status_202')],
//         ['503', getCount('status_503')],
//         ['405', getCount('status_405')],
//         ['400', getCount('status_400')],
//         ['411', getCount('status_411')],
//         ['413', getCount('status_413')],
//         ['500', getCount('status_500')],
//         ['other', getCount('status_other')],
//     ].filter(([, c]) => c > 0);
//
//     let statusTable = '\n📊 Status code distribution\n';
//     statusTable += '------------------------------------------\n';
//     statusTable += 'Code   Count         Percent\n';
//     statusTable += '------------------------------------------\n';
//     for (const [code, count] of rows) {
//         const pct = ((count / total) * 100);
//         statusTable += `${code.padEnd(6)} ${String(count).padEnd(13)} ${fmtPct(pct)}\n`;
//     }
//     statusTable += '------------------------------------------\n';
//     statusTable += `Total  ${String(total).padEnd(13)} 100.00 %\n\n`;
//
//     // tabela de latência
//     const lat = data.metrics['http_req_duration']?.values || {};
//     let latencyTable = '⏱️  HTTP Request Duration\n';
//     latencyTable += '----------------------------------------------------------------------------\n';
//     latencyTable += 'avg         min         med         max         p(90)       p(95)       p(99)\n';
//     latencyTable += '----------------------------------------------------------------------------\n';
//     latencyTable += `${fmtMs(lat.avg)}  ${fmtMs(lat.min)}  ${fmtMs(lat.med)}  ${fmtMs(lat.max)}  ${fmtMs(lat['p(90)'])}  ${fmtMs(lat['p(95)'])}  ${fmtMs(lat['p(99)'])}\n`;
//     latencyTable += '----------------------------------------------------------------------------\n\n';
//
//     return {
//         stdout: statusTable + latencyTable,
//         'summary.json': JSON.stringify(data, null, 2),
//     };
// }
//
// // --- helpers com clamp e unidades ---
// function clamp0(v) {
//     if (v === undefined || Number.isNaN(v)) return undefined;
//     // some sistemas podem reportar -0.00; normaliza
//     if (Object.is(v, -0)) return 0;
//     return Math.max(0, v);
// }
//
// function fmtMs(v) {
//     v = clamp0(v);
//     if (v === undefined) return '   -        ';
//     return (v.toFixed(2) + ' ms').padStart(11);
// }
//
// function fmtPct(v) {
//     v = clamp0(v);
//     if (v === undefined) return '  -   %';
//     return (v.toFixed(2) + ' %').padStart(7);
// }
